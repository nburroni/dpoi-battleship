socketInfo = {}

window.socketUtil =
  connect: ->
      $.get('/socket/url').done (socketUrl) ->
        socketInfo.socket = new WebSocket(socketUrl)
        socketInfo.socket.onopen = socketInfo.onopen
        socketInfo.socket.onmessage = socketInfo.onmessage

  onopen: (callback) ->
    socketInfo.onopen = callback
    socketInfo.socket.onopen = callback if socketInfo.socket?

  onmessage: (callback) ->
    socketInfo.onmessage = callback
    socketInfo.socket.onmessage = callback if socketInfo.socket?

  send: (data) -> socketInfo.socket.send JSON.stringify(data)