socketInfo = {}

window.socketUtil =
  connect: ->
      $.get('/socket/url').done (path) ->
        socketUrl = "ws://" + window.location.host + path
        socketInfo.socket = new WebSocket(socketUrl)
        socketInfo.socket.onopen = socketInfo.onopen
        socketInfo.socket.onmessage = socketInfo.onmessage

  onopen: (callback) ->
    socketInfo.onopen = callback
    socketInfo.socket.onopen = callback if socketInfo.socket?

  onmessage: (callback) ->
    callbackWrapper = (message) -> callback(JSON.parse(message.data))
    socketInfo.onmessage = callbackWrapper
    if socketInfo.socket?
      socketInfo.socket.onmessage = callbackWrapper

  send: (data) -> socketInfo.socket.send JSON.stringify(data)
  onclose: (callback) ->
    socketInfo.onclose = callback