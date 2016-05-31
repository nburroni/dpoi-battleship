angular.module 'app'
  .controller 'GameController', ['$scope', '$http',
    ($scope, $http) ->

      socket = window.socketUtil
      
      socket.onmessage (data) -> console.log(data)
      socket.connect()
      socket.onopen ->
        socket.send { action: "search-game" }


]