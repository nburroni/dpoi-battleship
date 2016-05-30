socketInfo = {}

connect = ->
    $.get('/socket/url').done (socketUrl) ->
      console.log socketUrl
      socketInfo.socket = new WebSocket(socketUrl)
      socketInfo.socket.onopen = socketInfo.onopen
      socketInfo.socket.onmessage = socketInfo.onopen

onopen = (callback) ->
  socketInfo.onopen = callback
  socketInfo.socket.onopen = callback if socketInfo.socket?

onmessage = (callback) ->
  socketInfo.onmessage = callback
  socketInfo.socket.onmessage = callback if socketInfo.socket?

sendToSocket = (data) -> socketInfo.send data.stringify