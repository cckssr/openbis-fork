/*
 *  Copyright ETH 2026 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

// Mirrors the path convention implemented by server-sftp's StandardPathLister/StandardPathTranslator
// (spaces/{code}/projects/{code}/experiments/{name (permId)}/samples|folders/{name (permId)}/datasets/{name (permId)}/files/...)
// so that a link built here resolves to the same node in the SFTP virtual file system.

const PROPERTY_NAME = 'NAME'
const FOLDER_SAMPLE_TYPE_CODE = 'FOLDER'

const TYPE_LABELS = {
  SPACE: 'spaces',
  PROJECT: 'projects',
  EXPERIMENT: 'experiments',
  SAMPLE: 'samples',
  FOLDER: 'folders',
  DATA_SET: 'datasets',
  FILE: 'files'
}

function getProperty(entity, code) {
  try {
    const properties = entity && entity.getProperties ? entity.getProperties() : null
    return properties ? properties[code] : null
  } catch (e) {
    return null
  }
}

function tryGet(entity, getterName) {
  try {
    return entity && entity[getterName] ? entity[getterName]() : null
  } catch (e) {
    return null
  }
}

function displayName(entity) {
  const name = getProperty(entity, PROPERTY_NAME)
  const permId = entity.getPermId().getPermId()
  return (name ? name + ' ' : '') + '(' + permId + ')'
}

function spaceSegments(space) {
  if (!space) {
    return null
  }
  return [TYPE_LABELS.SPACE, space.getCode()]
}

function projectSegments(project) {
  const space = project && tryGet(project, 'getSpace')
  const spacePath = spaceSegments(space)
  if (!spacePath) {
    return null
  }
  return [...spacePath, TYPE_LABELS.PROJECT, project.getCode()]
}

function experimentSegments(experiment) {
  const project = experiment && tryGet(experiment, 'getProject')
  const projectPath = projectSegments(project)
  if (!projectPath) {
    return null
  }
  return [...projectPath, TYPE_LABELS.EXPERIMENT, displayName(experiment)]
}

function sampleSegments(sample) {
  if (!sample) {
    return null
  }

  const type = tryGet(sample, 'getType')
  const typeLabel = type && type.getCode() === FOLDER_SAMPLE_TYPE_CODE ? TYPE_LABELS.FOLDER : TYPE_LABELS.SAMPLE

  const experiment = tryGet(sample, 'getExperiment')
  const project = tryGet(sample, 'getProject')
  const space = tryGet(sample, 'getSpace')

  const containerPath = experiment
    ? experimentSegments(experiment)
    : project
      ? projectSegments(project)
      : spaceSegments(space)

  if (!containerPath) {
    return null
  }
  return [...containerPath, typeLabel, displayName(sample)]
}

function dataSetSegments(dataSet) {
  if (!dataSet) {
    return null
  }

  const sample = tryGet(dataSet, 'getSample')
  const experiment = tryGet(dataSet, 'getExperiment')

  const containerPath = sample ? sampleSegments(sample) : experimentSegments(experiment)

  if (!containerPath) {
    return null
  }
  return [...containerPath, TYPE_LABELS.DATA_SET, displayName(dataSet)]
}

function entitySegments(kind, entity) {
  // objKind is spelled inconsistently by callers (ui-admin's objectType.js uses 'dataSet',
  // ELN-LIMS's hand-built props use 'dataset', both use plain 'object'/'collection'), so
  // normalize case before matching instead of relying on one canonical spelling.
  switch ((kind || '').toLowerCase()) {
    case 'object':
      return sampleSegments(entity)
    case 'collection':
      return experimentSegments(entity)
    case 'dataset':
      return dataSetSegments(entity)
    default:
      return null
  }
}

function normalizeAfsPathSegments(afsPath) {
  if (!afsPath) {
    return []
  }
  return afsPath.split('/').filter(segment => segment.length > 0)
}

function buildSftpUrl({ host, port, kind, entity, afsPath }) {
  if (!port) {
    return null
  }

  const rootSegments = entitySegments(kind, entity)
  if (!rootSegments) {
    return null
  }

  const segments = [...rootSegments, TYPE_LABELS.FILE, ...normalizeAfsPathSegments(afsPath)]
  const encodedPath = segments.map(encodeURIComponent).join('/')
  const resolvedHost = host || window.location.hostname

  return `sftp://${resolvedHost}:${port}/${encodedPath}`
}

export default {
  buildSftpUrl
}
