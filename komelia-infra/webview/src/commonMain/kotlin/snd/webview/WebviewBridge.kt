package snd.webview

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

fun interface WebviewCallback {
    fun run(id: String, request: String)

    @Serializable
    data class CallbackResponse<T>(val result: T)
}

@Serializable
data class WebviewMessage(
    val id: String,
    val method: String,
    val params: JsonArray
)

// adapted from https://github.com/webview/webview/blob/1e1298331e687e23871a61854a016df45c8e419c/core/include/webview/detail/engine_base.hh#L203
internal const val initScript = """
(function() {
  'use strict';
  var port;
  var initQueue = [];
  window.onmessage = function(e) {
    port = e.ports[0];
    initQueue.forEach((el) => port.postMessage(JSON.stringify(el)));
    initQueue = [];
    window.onmessage = undefined;
  }
  
  function generateId() {
    var crypto = window.crypto || window.msCrypto;
    var bytes = new Uint8Array(16);
    crypto.getRandomValues(bytes);
    return Array.prototype.slice.call(bytes).map(function(n) {
      var s = n.toString(16);
      return ((s.length % 2) == 1 ? '0' : '') + s;
    }).join('');
  }
  var Webview = (function() {
    var _promises = {};
    function Webview_() {}
    Webview_.prototype.call = function(method) {
      var _id = generateId();
      var _params = Array.prototype.slice.call(arguments, 1);
      var promise = new Promise(function(resolve, reject) {
        _promises[_id] = { resolve, reject };
      });
      var message = { id: _id, method: method, params: _params};
      if(port == undefined){
        initQueue.push(message);
      } else{
        port.postMessage(JSON.stringify(message))
      }
      return promise;
    };
    Webview_.prototype.onReply = function(id, status, result) {
      var promise = _promises[id];
      if (result !== undefined) {
        try {
          result = JSON.parse(result);
        } catch (e) {
          promise.reject(new Error("Failed to parse binding result as JSON"));
          return;
        }
      }
      if (status === 0) {
        promise.resolve(result);
      } else {
        promise.reject(result);
      }
    };
    Webview_.prototype.onBind = function(name) {
      if (window.hasOwnProperty(name)) {
        throw new Error('Property "' + name + '" already exists');
      }
      window[name] = (function() {
        var params = [name].concat(Array.prototype.slice.call(arguments));
        return Webview_.prototype.call.apply(this, params);
      }).bind(this);
    };
    Webview_.prototype.onUnbind = function(name) {
      if (!window.hasOwnProperty(name)) {
        throw new Error('Property "' + name + '" does not exist');
      }
      delete window[name];
    };
    return Webview_;
  })();
  window.__webview__ = new Webview();
})(); 
"""
