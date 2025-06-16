/* eslint-disable-next-line no-undef */
module.exports = function (api) {
  api.cache(true)

  const presets = ['@babel/preset-env', '@babel/preset-react', '@babel/preset-typescript']

  const plugins = [
    '@babel/plugin-transform-runtime',
    '@babel/plugin-transform-object-rest-spread',
    '@babel/plugin-transform-class-properties',
    'babel-plugin-transform-amd-to-commonjs'
  ]

  return {
    presets,
    plugins,
    sourceType: 'unambiguous'
  }
}
