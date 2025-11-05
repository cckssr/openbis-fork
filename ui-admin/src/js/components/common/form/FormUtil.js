import _ from 'lodash'

class FormUtil {
  createField(params = {}) {
    return {
      value: null,
      visible: true,
      enabled: true,
      ...params
    }
  }

  changeObjectField(object, field, value) {
    const newObject = {
      ...object,
      [field]: {
        ...object[field],
        value
      }
    }
    return {
      oldObject: object,
      newObject
    }
  }

  changeCollectionItemField(collection, itemId, field, value) {
    const index = collection.findIndex(item => item.id === itemId)

    const { oldObject, newObject } = this.changeObjectField(
      collection[index],
      field,
      value
    )

    const newCollection = Array.from(collection)
    newCollection[index] = newObject

    return {
      oldCollection: collection,
      newCollection,
      oldObject,
      newObject,
      index
    }
  }

  getFieldValue(object, path) {
    const field = !_.isNil(object) ? _.get(object, path, null) : null

    if (!_.isNil(field)) {
      const value = field.value
      if (_.isNil(value)) {
        return null
      } else if (_.isString(value) && _.isEmpty(value.trim())) {
        return null
      } else {
        return value
      }
    } else {
      return null
    }
  }

  hasFieldChanged(currentObject, originalObject, path) {
    const currentValue = this.getFieldValue(currentObject, path)
    const originalValue = this.getFieldValue(originalObject, path)
    
    // Use deep equality check for objects (including arrays)
    if (_.isObject(currentValue) && _.isObject(originalValue)) {
      return !this.areObjectsEqual(currentValue, originalValue)
    }
    
    return originalValue !== currentValue
  }

  haveFieldsChanged(currentObject, originalObject, paths) {
    return _.some(paths, path =>
      this.hasFieldChanged(currentObject, originalObject, path)
    )
  }

  areObjectsEqual(obj1, obj2) {
    // Handle null/undefined cases
    if (_.isNil(obj1) && _.isNil(obj2)) {
      return true
    }
    if (_.isNil(obj1) || _.isNil(obj2)) {
      return false
    }

    // If both are not objects, use simple equality
    if (!_.isObject(obj1) || !_.isObject(obj2)) {
      return obj1 === obj2
    }

    // Handle arrays
    if (Array.isArray(obj1) && Array.isArray(obj2)) {
      if (obj1.length !== obj2.length) {
        return false
      }
      return obj1.every((item, index) => this.areObjectsEqual(item, obj2[index]))
    }

    // If one is array and other is not
    if (Array.isArray(obj1) !== Array.isArray(obj2)) {
      return false
    }

    // Get all keys from both objects
    const keys1 = Object.keys(obj1)
    const keys2 = Object.keys(obj2)

    // Check if number of keys match
    if (keys1.length !== keys2.length) {
      return false
    }

    // Check each key and value
    for (const key of keys1) {
      if (!(key in obj2)) {
        return false
      }

      const val1 = obj1[key]
      const val2 = obj2[key]

      // Recursively compare nested objects
      if (_.isObject(val1) || _.isObject(val2)) {
        if (!this.areObjectsEqual(val1, val2)) {
          return false
        }
      } else {
        // Compare primitive values
        if (val1 !== val2) {
          return false
        }
      }
    }

    return true
  }

  trimFields(object) {
    const trimString = str => {
      const trimmed = str.trim()
      return trimmed.length > 0 ? trimmed : null
    }

    const trimField = field => {
      if (field) {
        if (_.isString(field)) {
          return trimString(field)
        } else if (_.isObject(field) && _.isString(field.value)) {
          return {
            ...field,
            value: trimString(field.value)
          }
        }
      }
      return field
    }

    return _.mapValues(
      {
        ...object
      },
      trimField
    )
  }

  transformMetadataToObject(metadataValue) {
    let metadataObject = {}
    if (metadataValue && Array.isArray(metadataValue)) {
      metadataValue.forEach(item => {
        if (item.key && item.value !== undefined) {
          metadataObject[item.key] = item.value
        }
      })
    } else if (metadataValue && typeof metadataValue === 'object') {
      metadataObject = metadataValue
    }
    return metadataObject
  }
}

export default new FormUtil()
