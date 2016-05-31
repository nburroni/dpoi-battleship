socketInfo = {}

window.socketUtil =
  connect: ->
      $.get('/socket/url').done (socketUrl) ->
        console.log socketUrl
        socketInfo.socket = new WebSocket(socketUrl)
        socketInfo.socket.onopen = socketInfo.onopen
        socketInfo.socket.onmessage = socketInfo.onmessage

  onopen: (callback) ->
    socketInfo.onopen = callback
    socketInfo.socket.onopen = callback if socketInfo.socket?

  onmessage: (callback) ->
    socketInfo.onmessage = callback
    socketInfo.socket.onmessage = callback if socketInfo.socket?

  sendToSocket: (data) -> socketInfo.socket.send JSON.stringify(data)

$ ->
  window.socketUtil.onmessage (data) -> console.log(data)
  window.socketUtil.connect()
  window.socketUtil.onopen ->
    console.log("Open!")
    window.socketUtil.sendToSocket { msg: "Hi socket!" }