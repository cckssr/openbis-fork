;(function(global){
  'use strict'

/**
 * ======================================================
 * OpenBIS Data Store Server facade internal code (DO NOT USE!!!)
 * ======================================================
 */

function _DataStoreServerInternal(datastoreUrlOrNull, httpServerUri){
	this.init(datastoreUrlOrNull, httpServerUri);
}

_DataStoreServerInternal.prototype.init = function(datastoreUrlOrNull, httpServerUri){
	this.datastoreUrl = this.normalizeUrl(datastoreUrlOrNull, httpServerUri) + "/api";
	this.httpServerUri = httpServerUri;
}

_DataStoreServerInternal.prototype.log = function(msg){
	if(console){
		console.log(msg);
	}
}

_DataStoreServerInternal.prototype.normalizeUrl = function(openbisUrlOrNull, httpServerUri){
	var parts = this.parseUri(window.location);
	
	if(openbisUrlOrNull){
		var openbisParts = this.parseUri(openbisUrlOrNull);
		
		for(var openbisPartName in openbisParts){
			var openbisPartValue = openbisParts[openbisPartName];
			
			if(openbisPartValue){
				parts[openbisPartName] = openbisPartValue;
			}
		}
	}
	
	return parts.protocol + "://" + parts.authority + (httpServerUri || parts.path);
}

_DataStoreServerInternal.prototype.getUrlForMethod = function(method) {
    return this.datastoreUrl + "?method=" + method;
}

_DataStoreServerInternal.prototype.jsonRequestData = function(params) {
	return JSON.stringify(params);
}

_DataStoreServerInternal.prototype.sendHttpRequest = function(httpMethod, contentType, url, data) {
    const { promise, abortFn } = this._internal.sendHttpRequestAbortable(httpMethod, contentType, url, data)
    return promise
}

_DataStoreServerInternal.prototype.sendHttpRequestAbortable = function(httpMethod, contentType, url, data) {
	const xhr = new XMLHttpRequest();
	xhr.open(httpMethod, url);
	xhr.responseType = "blob";
    // Set a timeout, 30 seconds
	xhr.timeout = 30000; 

    let abortFn;

	const promise = new Promise((resolve, reject) => {
		xhr.onreadystatechange = function() {
			if (xhr.readyState === XMLHttpRequest.DONE) {
				const status = xhr.status;
				const response = xhr.response;                

				if (status >= 200 && status < 300) {
					const contentType = this.getResponseHeader('content-type');

					switch (contentType) {
						case 'text/plain':
							// Fall through.
						case'application/json': {
							response.text().then((blobResponse) => resolve(blobResponse))
								.catch((error) => reject(error));
							break;
						}
						case 'application/octet-stream': {
							resolve(response);
							break;
						}
						default: {
							reject(new Error("Client error HTTP response. Unsupported content-type received."));
							break;
						}
					}
				} else if (status >= 400 && status < 600) {
					response.text().then((textResponse) => {
						try {
							const errorMessage = JSON.parse(textResponse).error[1].message;
							reject(new Error(errorMessage));
						} catch (e) {
							reject(new Error(textResponse || xhr.statusText));
						}
					}).catch(() => {
						reject(new Error("HTTP Error: " + status));
					});
				}
			}
		};

        // Handle network errors
		xhr.onerror = function() {
			reject(new Error("Network error: Unable to reach the server."));
		};

		// Handle timeout errors
		xhr.ontimeout = function() {
			reject(new Error("Request timed out: The server did not respond."));
		};

        abortFn = () => {
            xhr.abort();
            reject(new Error("Request aborted by user."));
        };

		xhr.send(data);
	});

    return { promise, abortFn: abortFn };
}



  _DataStoreServerInternal.prototype.buildGetUrl = function(queryParams) {
	const queryString = Object.keys(queryParams)
	  .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(queryParams[key])}`)
	  .join('&');
	return `${this.datastoreUrl}?${queryString}`;
  }



// Functions for working with cookies (see http://www.quirksmode.org/js/cookies.html)

_DataStoreServerInternal.prototype.createCookie = function(name,value,days) {
	if (days) {
		var date = new Date();
		date.setTime(date.getTime()+(days*24*60*60*1000));
		var expires = "; expires="+date.toGMTString();
	}
	else var expires = "";
	document.cookie = name+"="+value+expires+"; path=/";
}

_DataStoreServerInternal.prototype.readCookie = function(name) {
	var nameEQ = name + "=";
	var ca = document.cookie.split(';');
	for(var i=0;i < ca.length;i++) {
		var c = ca[i];
		while (c.charAt(0)==' ') c = c.substring(1,c.length);
		if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
	}
	return null;
}

_DataStoreServerInternal.prototype.eraseCookie = function(name) {
	this.createCookie(name,"",-1);
}

// parseUri 1.2.2 (c) Steven Levithan <stevenlevithan.com> MIT License (see http://blog.stevenlevithan.com/archives/parseuri)

_DataStoreServerInternal.prototype.parseUri = function(str) {
	var options = {
		strictMode: false,
		key: ["source","protocol","authority","userInfo","user","password","host","port","relative","path","directory","file","query","anchor"],
		q:   {
			name:   "queryKey",
			parser: /(?:^|&)([^&=]*)=?([^&]*)/g
		},
		parser: {
			strict: /^(?:([^:\/?#]+):)?(?:\/\/((?:(([^:@]*)(?::([^:@]*))?)?@)?([^:\/?#]*)(?::(\d*))?))?((((?:[^?#\/]*\/)*)([^?#]*))(?:\?([^#]*))?(?:#(.*))?)/,
			loose:  /^(?:(?![^:@]+:[^:@\/]*@)([^:\/?#.]+):)?(?:\/\/)?((?:(([^:@]*)(?::([^:@]*))?)?@)?([^:\/?#]*)(?::(\d*))?)(((\/(?:[^?#](?![^?#\/]*\.[^?#\/.]+(?:[?#]|$)))*\/?)?([^?#\/]*))(?:\?([^#]*))?(?:#(.*))?)/
		}
	};
	
	var	o   = options,
		m   = o.parser[o.strictMode ? "strict" : "loose"].exec(str),
		uri = {},
		i   = 14;

	while (i--) uri[o.key[i]] = m[i] || "";

	uri[o.q.name] = {};
	uri[o.key[12]].replace(o.q.parser, function ($0, $1, $2) {
		if ($1) uri[o.q.name][$1] = $2;
	});

	return uri;
}


/** Helper method for checking response from DSS server */
function parseJsonResponse(rawResponse) {
	return new Promise((resolve, reject) => {
		let response = JSON.parse(rawResponse);
		if (response.error) {
			reject(new Error(response.error[1].message));
		} else {
			resolve(response);
		}
	});
}



/**
 * ===============
 * DSS facade
 * ===============
 * 
 * The facade provides access to the DSS methods
 * 
 */
function DataStoreServer(datastoreUrlOrNull, httpServerUri) {
	this._internal = new _DataStoreServerInternal(datastoreUrlOrNull, httpServerUri);
}


/**
 * ==================================================================================
 * ch.ethz.sis.afsapi.api.AuthenticationAPI methods
 * ==================================================================================
 */

/**
 * Stores the current session in a cookie. 
 *
 * @method
 */
DataStoreServer.prototype.rememberSession = function() {
	this._internal.createCookie('dataStoreServer', this.getSession(), 1);
}

/**
 * Removes the current session from a cookie. 
 *
 * @method
 */
DataStoreServer.prototype.forgetSession = function() {
	this._internal.eraseCookie('dataStoreServer');
}

/**
 * Restores the current session from a cookie.
 *
 * @method
 */
DataStoreServer.prototype.restoreSession = function() {
	this._internal.sessionToken = this._internal.readCookie('dataStoreServer');
}

/**
 * Sets the current session.
 *
 * @method
 */
DataStoreServer.prototype.useSession = function(sessionToken){
	this._internal.sessionToken = sessionToken;
}

/**
 * Returns the current session.
 * 
 * @method
 */
DataStoreServer.prototype.getSession = function(){
	return this._internal.sessionToken;
}

/**
 * Sets interactiveSessionKey.
 * 
 * @method
 */
DataStoreServer.prototype.setInteractiveSessionKey = function(interactiveSessionKey){
	this._internal.interactiveSessionKey = interactiveSessionKey;
}

/**
 * Returns the current session.
 * 
 * @method
 */
DataStoreServer.prototype.getInteractiveSessionKey = function(){
	return this._internal.interactiveSessionKey;
}

/**
 * Sets transactionManagerKey.
 * 
 * @method
 */
DataStoreServer.prototype.setTransactionManagerKey = function(transactionManagerKey){
	this._internal.transactionManagerKey = transactionManagerKey;
}

/**
 * Returns the current session.
 * 
 * @method
 */
DataStoreServer.prototype.getTransactionManagerKey = function(){
	return this._internal.transactionManagerKey;
}

DataStoreServer.prototype.fillCommonParameters = function(params) {
	if(this.getSession()) {
		params["sessionToken"] = this.getSession();
	}
	if(this.getInteractiveSessionKey()) {
		params["interactiveSessionKey"] = this.getInteractiveSessionKey();
	}
	if(this.getTransactionManagerKey()) {
		params["transactionManagerKey"] = this.getTransactionManagerKey();
	}
	return params;
}

const encodeParams = (params) => {
	return Object.entries(params)
		.map(kv => {
			const key = kv[0]
			const value =  kv[1]
			const encodedValue = (key === "data")
				? value : encodeURIComponent(value)
			return `${encodeURIComponent(key)}=${encodedValue}`
		})
		.join("&")
};

/**
 * Log into DSS.
 * 
 * @method
 */
DataStoreServer.prototype.login = function(userId, userPassword) {
	var datastoreObj = this
	const data =  this.fillCommonParameters({
		"method": "login",
		"userId": userId,
		"password": userPassword
	});
	return this._internal.sendHttpRequest(
		"POST",
		"application/octet-stream",
		this._internal.datastoreUrl,
		encodeParams(data)
	).then((loginResponse) => {
		return new Promise((resolve, reject) => {
			datastoreObj._internal.sessionToken = loginResponse;
			datastoreObj.rememberSession();
			resolve(loginResponse);
		})
	});
}


/**
 * Checks whether the current session is still active.
 *
 */
DataStoreServer.prototype.isSessionValid = function() {
	return new Promise((resolve, reject) => {
		if (this.getSession()) {
			const data =  this.fillCommonParameters({"method":"isSessionValid"});
			this._internal.sendHttpRequest(
				"GET",
				"application/octet-stream",
				this._internal.datastoreUrl,
				encodeParams(data)
			).then((response) => parseJsonResponse(response).then((value) => resolve(value))
				.catch((reason) => reject(reason)));
		} else {
			resolve({ result : false })
		}
	});
}

/**
 * Restores the current session from a cookie.
 * 
 * @see restoreSession()
 * @see isSessionActive()
 * @method
 */
DataStoreServer.prototype.ifRestoredSessionActive = function() {
	this.restoreSession();
	return this.isSessionValid();
}

/**
 * Log out of DSS.
 * 
 * @method
 */
DataStoreServer.prototype.logout = function() {
	return new Promise((resolve, reject) => {
		this.forgetSession();

		if (this.getSession()) {
			const data = this.fillCommonParameters({"method": "logout"});
			this._internal.sendHttpRequest(
				"POST",
				"application/octet-stream",
				this._internal.datastoreUrl,
				encodeParams(data)
			).then((response) => parseJsonResponse(response).then((value) => resolve(value))
				.catch((reason) => reject(reason)));
		} else {
			resolve({result: null});
		}
	});
}


/**
 * ==================================================================================
 * ch.ethz.sis.afsapi.api.OperationsAPI methods
 * ==================================================================================
 */

/**
 * List files in the DSS for given owner and source
 */
DataStoreServer.prototype.list = function(owner, source, recursively){
	const data =  this.fillCommonParameters({
		"method": "list",
		"owner" :  owner,
		"source":  source,
		"recursively":  recursively
	});
	const {promise, abortFn} = this._internal.sendHttpRequestAbortable(
		"GET",
		"application/octet-stream",
		this._internal.buildGetUrl(data),
		{}
	)
    return {
        promise : promise.then((response) => response.text()).then(
            (responseText) => FileEncoderDecoder.decodeFiles(responseText)
        ),
        abortFn
    };
}

/**
 * Read the contents of selected file
 * @param {chunk} chunks of the file
 */
DataStoreServer.prototype._read = function(chunks){
	const data =  this.fillCommonParameters({
		"method": "read"
	});
	const original = this._internal.sendHttpRequestAbortable(
		"POST",
		"application/octet-stream",
		this._internal.buildGetUrl(data),
		ChunkEncoderDecoder.encodeChunks(chunks) // Encode Chunks
	);

    original.promise = original.promise.then(function(result) {
        if (!(result instanceof Blob)) {
            throw new TypeError('_read result is not a valid value of type Blob');
        }
        return result.text();
    }).then(function(text) {
        var chunks = ChunkEncoderDecoder.decodeChunks(text); // Decode Chunks
        var data = chunks[0].getData();
        return new Blob([data]);
    });

	return original;
}

/**
 * Read the contents of selected file
 * @param {str} owner owner of the file 
 * @param {str} source path to file
 * @param {int} offset offset from which to start reading
 * @param {int} limit how many characters to read
 */
DataStoreServer.prototype.read = function(owner, source, offset, limit){
	return this._read([new Chunk(owner, source, offset, limit, ChunkEncoderDecoder.EMPTY_ARRAY)]);
}

/**
 * Read the contents of selected file
 * @param {chunk} chunks of the file
 */
DataStoreServer.prototype._write = function(chunks){
	const data =  this.fillCommonParameters({
		"method": "write"
	});
	const result = this._internal.sendHttpRequestAbortable(
		"POST",
		"application/octet-stream",
		this._internal.buildGetUrl(data),
		ChunkEncoderDecoder.encodeChunks(chunks) // Encode Chunks
	);
	return result;
}

/**
 * Write data to file (or create it)
 * @param {str} owner owner of the file
 * @param {str} source path to file
 * @param {int} offset offset from which to start writing
 * @param {str} data data to write
 */
DataStoreServer.prototype.write = function(owner, source, offset, data){
	return this._write([new Chunk(owner, source, offset, data.length, data)]);
}

/**
 * Delete file from the DSS
 * @param {str} owner owner of the file
 * @param {str} source path to file
 */
DataStoreServer.prototype.delete = function(owner, source){
	const data =  this.fillCommonParameters({
		"method": "delete",
		"owner" : owner,
		"source": source
	});
	return this._internal.sendHttpRequestAbortable(
		"DELETE",
		"application/octet-stream",
		this._internal.datastoreUrl,
		encodeParams(data)
	);
}

/**
 * Copy file within DSS
 */
DataStoreServer.prototype.copy = function(sourceOwner, source, targetOwner, target){
	const data =  this.fillCommonParameters({
		"method": "copy",
		"sourceOwner" : sourceOwner,
		"source": source,
		"targetOwner": targetOwner,
		"target" : target
	});
	return this._internal.sendHttpRequestAbortable(
		"POST",
		"application/octet-stream",
		this._internal.datastoreUrl,
		encodeParams(data)
	);
}

/**
 * Move file within DSS
 */
DataStoreServer.prototype.move = function(sourceOwner, source, targetOwner, target){
	const data =  this.fillCommonParameters({
		"method": "move",
		"sourceOwner" : sourceOwner,
		"source": source,
		"targetOwner": targetOwner,
		"target" : target
	});
	return this._internal.sendHttpRequestAbortable(
		"POST",
		"application/octet-stream",
		this._internal.datastoreUrl,
		encodeParams(data)
	);
}

/**
 * Create a file/directory within DSS
 */
DataStoreServer.prototype.create = function(owner, source, directory){
	const data =  this.fillCommonParameters({
		"method": "create",
		"owner" : owner,
		"source": source,
		"directory": directory
	});
	return this._internal.sendHttpRequestAbortable(
		"POST",
		"application/octet-stream",
		this._internal.datastoreUrl,
		encodeParams(data)
	);
}

/**
 * Get the space information for given owner and source
 */
DataStoreServer.prototype.free = function(owner, source){
	const data =  this.fillCommonParameters({
		"method": "free",
		"owner" :  owner,
		"source":  source,
	});
	const {promise, abortFn} = this._internal.sendHttpRequestAbortable(
		"GET",
		"application/octet-stream",
		this._internal.buildGetUrl(data),
		{}
	)
    return {promise :promise.then((response) => parseJsonResponse(response)).then((response) => {
        if(response && Array.isArray(response.result) && response.result.length === 2){
            return new FreeSpace(response.result[1])
        } else {
            return response
        }
    }),
    abortFn}
}

/**
 * ==================================================================================
 * ch.ethz.sis.afsapi.api.TwoPhaseTransactionAPI methods
 * ==================================================================================
 */

DataStoreServer.prototype.begin = function(transactionId){
	const data =  this.fillCommonParameters({
		"method": "begin",
		"transactionId" : transactionId
	});
	return this._internal.sendHttpRequest(
		"POST",
		"application/octet-stream",
		this._internal.datastoreUrl,
		encodeParams(data)
	);
}

DataStoreServer.prototype.prepare = function(){
	const data =  this.fillCommonParameters({
		"method": "prepare"
	});
	return this._internal.sendHttpRequest(
		"POST",
		"application/octet-stream",
		this._internal.datastoreUrl,
		encodeParams(data)
	);
}

DataStoreServer.prototype.commit = function(){
	const data =  this.fillCommonParameters({
		"method": "commit"
	});
	return this._internal.sendHttpRequest(
		"POST",
		"application/octet-stream",
		this._internal.datastoreUrl,
		encodeParams(data)
	);
}


DataStoreServer.prototype.rollback = function(){
	const data =  this.fillCommonParameters({
		"method": "rollback"
	});
	return this._internal.sendHttpRequest(
		"POST",
		"application/octet-stream",
		this._internal.datastoreUrl,
		encodeParams(data)
	);
}

DataStoreServer.prototype.recover = function(){
	const data =  this.fillCommonParameters({
		"method": "recover"
	});
	return this._internal.sendHttpRequest(
		"POST",
		"application/octet-stream",
		this._internal.datastoreUrl,
		encodeParams(data)
	);
}

/**
 * ==================================================================================
 * DTO
 * ==================================================================================
 */

var Chunk = function(owner, source, offset, limit, data) {
    if (!(typeof owner === "string") && !(owner instanceof String)) {
        throw new TypeError('owner is not a valid value of type string');
    }
    if (!(typeof source === "string") && !(source instanceof String)) {
        throw new TypeError('source is not a valid value of type string');
    }
    if (!Number.isInteger(offset)) {
        throw new TypeError('offset is not a valid value of type Integer');
    }
    if (!Number.isInteger(limit)) {
        throw new TypeError('limit is not a valid value of type Integer');
    }
    if (!(data instanceof Uint8Array) || data === undefined || data === null) {
        throw new TypeError('data is not a valid value of type Uint8Array');
    }
    this.owner = owner;
    this.source = source;
    this.offset = offset;
    this.limit = limit;
    this.data = data;

    this.getOwner = function(){
        return this.owner;
    }
    this.getSource = function(){
        return this.source;
    }
    this.getOffset = function(){
        return this.offset;
    }
    this.getLimit = function(){
        return this.limit;
    }
    this.getData = function(){
        return this.data;
    }
}

var File = function(fileObject){
    this.owner = fileObject.owner;
    this.path = fileObject.path;
    this.name = fileObject.name;
    this.directory = fileObject.directory;
    this.size = fileObject.size;
    if ( typeof fileObject.lastModifiedTime === "number" ) {
        this.lastModifiedTime = fileObject.lastModifiedTime;
    } else if ( typeof fileObject.lastModifiedTime === "string" ) {
        this.lastModifiedTime = Date.parse(fileObject.lastModifiedTime);
    } else {
        this.lastModifiedTime = null;
    }


    this.getOwner = function(){
        return this.owner;
    }
    this.getPath = function(){
        return this.path;
    }
    this.getName = function(){
        return this.name;
    }
    this.getDirectory = function(){
        return this.directory;
    }
    this.getSize = function(){
        return this.size;
    }
    this.getLastModifiedTime = function(){
        return this.lastModifiedTime;
    }
}

var FreeSpace = function(freeSpaceObject){

    this.free = freeSpaceObject.free;
    this.total = freeSpaceObject.total;

    this.getFree = function(){
        return this.free;
    }
    this.getTotal = function(){
        return this.total;
    }
}

/**
 * ==================================================================================
 * UTILITY CLASSES
 * ==================================================================================
 */

var ChunkEncoderDecoder = (function(){
    const CHUNK_SEPARATOR = ',';
    const CHUNK_ARRAY_SEPARATOR = ';';
    const EMPTY_ARRAY = new Uint8Array();

    function encodeChunk(chunk) {
        var dataAsBase64 = Base64.bytesToBase64(chunk.getData());
        return chunk.getOwner() + CHUNK_SEPARATOR + chunk.getSource() + CHUNK_SEPARATOR + chunk.getOffset() + CHUNK_SEPARATOR + chunk.getLimit() + CHUNK_SEPARATOR + dataAsBase64;
    }

    function encodeChunks(chunks) {
        var builder = '';
        for (var cIdx = 0; cIdx < chunks.length; cIdx++) {
            if (cIdx > 0) {
                builder += CHUNK_ARRAY_SEPARATOR;
            }
            builder += encodeChunk(chunks[cIdx]);
        }
        return builder;
    }

    function decodeChunk(chunkAsString) {
        var chunkParameters = chunkAsString.split(CHUNK_SEPARATOR);

        var data = null;
        if (chunkParameters.length == 5) {
            data = Base64.base64ToBytes(chunkParameters[4]);
        } else {
            data = EMPTY_ARRAY;
        }

        return new Chunk(chunkParameters[0],
                chunkParameters[1],
                parseInt(chunkParameters[2]),
                parseInt(chunkParameters[3]),
                data);
    }

    function decodeChunks(chunksAsString) {
        var chunksParameters = chunksAsString.split(CHUNK_ARRAY_SEPARATOR);
        var chunks = [];
        for (var cIdx = 0; cIdx < chunksParameters.length; cIdx++) {
            chunks[cIdx] = decodeChunk(chunksParameters[cIdx]);
        }
        return chunks;
    }

    return {
        encodeChunks : encodeChunks,
        decodeChunks : decodeChunks,
        EMPTY_ARRAY : EMPTY_ARRAY
    }
})();

var FileEncoderDecoder = (function(){
    const FILE_SEPARATOR = ',';
    const FILE_ARRAY_SEPARATOR = ';';

    function encodeFile(file) {
        return file.getOwner() + FILE_SEPARATOR
            + file.getPath() + FILE_SEPARATOR
            + file.getName() + FILE_SEPARATOR
            + ( file.getDirectory() != null ? file.getDirectory() : "" ) + FILE_SEPARATOR
            + ( file.getSize() != null ? file.getSize() : "" ) + FILE_SEPARATOR
            + ( file.getLastModifiedTime() != null ? file.getLastModifiedTime() : "" );
    }

    function encodeFiles(files) {
        var builder = '';
        for (var fIdx = 0; fIdx < files.length; fIdx++) {
            if (fIdx > 0) {
                builder += FILE_ARRAY_SEPARATOR;
            }
            builder += encodeFile(files[fIdx]);
        }
        return builder;
    }

    function decodeFile(fileAsString) {
        var fileParameters = fileAsString.split(FILE_SEPARATOR);

        var directory = (fileParameters[3].length != 0) ? (fileParameters[3].toLowerCase() === 'true') : null;
        var size = (fileParameters[4].length != 0) ? Number.parseInt(fileParameters[4]) : null;
        var lastModifiedTime = (fileParameters[5].length != 0) ? Number.parseInt(fileParameters[5]) : null;

        return new File({
            owner: fileParameters[0],
            path: fileParameters[1],
            name: fileParameters[2],
            directory: directory,
            size: size,
            lastModifiedTime: lastModifiedTime
        });
    }

    function decodeFiles(filesAsString) {
        var files = [];
        if (filesAsString.length != 0) {
            var filesParameters = filesAsString.split(FILE_ARRAY_SEPARATOR);
            for (var fIdx = 0; fIdx < filesParameters.length; fIdx++) {
                files[fIdx] = decodeFile(filesParameters[fIdx]);
            }
        }
        return files;
    }

    return {
        encodeFiles : encodeFiles,
        decodeFiles : decodeFiles
    }
})();


var Base64 = (function(){
/*
MIT License
Copyright (c) 2020 Egor Nepomnyaschih
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

/*
// This constant can also be computed with the following algorithm:
const base64abc = [],
	A = "A".charCodeAt(0),
	a = "a".charCodeAt(0),
	n = "0".charCodeAt(0);
for (let i = 0; i < 26; ++i) {
	base64abc.push(String.fromCharCode(A + i));
}
for (let i = 0; i < 26; ++i) {
	base64abc.push(String.fromCharCode(a + i));
}
for (let i = 0; i < 10; ++i) {
	base64abc.push(String.fromCharCode(n + i));
}
base64abc.push("+");
base64abc.push("/");
*/
const base64abc = [
	"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
	"N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
	"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
	"n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
	"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "+", "/"
];

/*
// This constant can also be computed with the following algorithm:
const l = 256, base64codes = new Uint8Array(l);
for (let i = 0; i < l; ++i) {
	base64codes[i] = 255; // invalid character
}
base64abc.forEach((char, index) => {
	base64codes[char.charCodeAt(0)] = index;
});
base64codes["=".charCodeAt(0)] = 0; // ignored anyway, so we just need to prevent an error
*/
const base64codes = [
	255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
	255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
	255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 62, 255, 255, 255, 63,
	52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 255, 255, 255, 0, 255, 255,
	255, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
	15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 255, 255, 255, 255, 255,
	255, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
	41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51
];

function getBase64Code(charCode) {
	if (charCode >= base64codes.length) {
		throw new Error("Unable to parse base64 string.");
	}
	const code = base64codes[charCode];
	if (code === 255) {
		throw new Error("Unable to parse base64 string.");
	}
	return code;
}

function bytesToBase64(bytes) {
	let result = '', i, l = bytes.length;
	for (i = 2; i < l; i += 3) {
		result += base64abc[bytes[i - 2] >> 2];
		result += base64abc[((bytes[i - 2] & 0x03) << 4) | (bytes[i - 1] >> 4)];
		result += base64abc[((bytes[i - 1] & 0x0F) << 2) | (bytes[i] >> 6)];
		result += base64abc[bytes[i] & 0x3F];
	}
	if (i === l + 1) { // 1 octet yet to write
		result += base64abc[bytes[i - 2] >> 2];
		result += base64abc[(bytes[i - 2] & 0x03) << 4];
		result += "==";
	}
	if (i === l) { // 2 octets yet to write
		result += base64abc[bytes[i - 2] >> 2];
		result += base64abc[((bytes[i - 2] & 0x03) << 4) | (bytes[i - 1] >> 4)];
		result += base64abc[(bytes[i - 1] & 0x0F) << 2];
		result += "=";
	}
	return result;
}

function base64ToBytes(str) {
	if (str.length % 4 !== 0) {
		throw new Error("Unable to parse base64 string.");
	}
	const index = str.indexOf("=");
	if (index !== -1 && index < str.length - 2) {
		throw new Error("Unable to parse base64 string.");
	}
	let missingOctets = str.endsWith("==") ? 2 : str.endsWith("=") ? 1 : 0,
		n = str.length,
		result = new Uint8Array(3 * (n / 4)),
		buffer;
	for (let i = 0, j = 0; i < n; i += 4, j += 3) {
		buffer =
			getBase64Code(str.charCodeAt(i)) << 18 |
			getBase64Code(str.charCodeAt(i + 1)) << 12 |
			getBase64Code(str.charCodeAt(i + 2)) << 6 |
			getBase64Code(str.charCodeAt(i + 3));
		result[j] = buffer >> 16;
		result[j + 1] = (buffer >> 8) & 0xFF;
		result[j + 2] = buffer & 0xFF;
	}
	return result.subarray(0, result.length - missingOctets);
}

return {
    bytesToBase64 : bytesToBase64,
    base64ToBytes : base64ToBytes
};

})();

/**
 * ==================================================================================
 * ACCESS TO PRIVATE CLASSES
 * ==================================================================================
 */

DataStoreServer.prototype.Private = {};
DataStoreServer.prototype.Private.FreeSpace = FreeSpace;
DataStoreServer.prototype.Private.File = File;
DataStoreServer.prototype.Private.Chunk = Chunk;

DataStoreServer.prototype.Private.ChunkEncoderDecoder = ChunkEncoderDecoder;
DataStoreServer.prototype.Private.FileEncoderDecoder = FileEncoderDecoder;
DataStoreServer.prototype.Private.Base64 = Base64;

/**
 * ==================================================================================
 * EXPORT
 * ==================================================================================
 */

if (typeof define === 'function' && define.amd) {
  define(function () {
    return DataStoreServer
  })
} else if (typeof module === 'object' && module.exports) {
  module.exports = DataStoreServer
} else {
  global.DataStoreServer = DataStoreServer
}

})(this);
