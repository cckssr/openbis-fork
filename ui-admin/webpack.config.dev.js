/* eslint-disable */
const Webpack = require('webpack')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const ReactRefreshWebpackPlugin = require('@pmmmwh/react-refresh-webpack-plugin');
const path = require('path')

module.exports = {
  entry: './src/js/index.js',
  output: {
    path: __dirname + '/build/npm-build/',
    filename: 'bundle.js'
  },

  devServer: {
    client: {
      overlay: {
        // explanation at https://sentry.io/answers/react-resizeobserver-loop-completed-with-undelivered-notifications/
        runtimeErrors: (error) => {
          const ignoreErrors = [
            "ResizeObserver loop limit exceeded",
            "ResizeObserver loop completed with undelivered notifications.",
          ];
          if (ignoreErrors.includes(error.message)) {
            return false;
          }
          return true;
        },
      },
    },
    host: '0.0.0.0',
    port: 9999,
    server: {
      type: 'https'
    },
    proxy: [{
      context: ['/openbis'],
      target: 'http://localhost:8888',
      pathRewrite: { '^/openbis/resources': '/openbis-test/resources' },
      changeOrigin: true,
      secure: false
    }
    ],
    devMiddleware: {
      publicPath: '/admin/'
    },
    static: [
      {
        directory: './src/resources',
        publicPath: '/admin'
      }
    ]
  },

  devtool: 'source-map',

  mode: 'development',

  module: {
    rules: [
      {
        test: /\.(js|jsx|ts|tsx)$/,
        exclude: /node_modules/,
        use: [
          {
            loader: 'babel-loader',
            options: {
              plugins: [require.resolve('react-refresh/babel')].filter(Boolean),
            },
          },
        ],
      },
      {
        test: /\.(css)$/,
        use: ['style-loader', 'css-loader']
      },
      {
        test: /\.(png|svg|jpg|jpeg|gif|ico)$/i,
        type: 'asset'
      },
      {
        test: /\.(woff2?|eot|ttf|otf)(\?.*)?$/,
        loader: 'url-loader',
        options: {
          limit: 10000
        }
      }
    ]
  },

  resolve: {
    alias: {
      '@src': path.resolve(__dirname, 'src/'),
      '@srcTest': path.resolve(__dirname, 'srcTest/'),
      '@srcV3': path.resolve(__dirname, 'srcV3/'),
      '@srcDss': path.resolve(__dirname, 'srcDss/')
    },
    fallback: {
      stream: require.resolve('stream-browserify'),
      buffer: require.resolve('buffer')
    }
  },

  plugins: [
    new ReactRefreshWebpackPlugin(),
    new HtmlWebpackPlugin({
      inject: 'body',
      filename: './index.html',
      template: './index.html'
    }),
    new Webpack.WatchIgnorePlugin({
      paths: [new RegExp('/node_modules/'), new RegExp('/node/')]
    }),
    new Webpack.ProvidePlugin({
      Buffer: ['buffer', 'Buffer']
    }),
    new Webpack.ProvidePlugin({
      process: 'process/browser'
    })
  ]
}
