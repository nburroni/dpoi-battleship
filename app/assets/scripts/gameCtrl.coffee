angular.module 'app'
  .controller 'GameController', ['$scope', '$http',
    ($scope, $http) ->

      $scope.startGame = false

      socket = window.socketUtil
      
      socket.onmessage (data) -> $scope.handleMessage(data.msg)
      socket.connect()
      socket.onopen ->
        socket.send { action: "search-game" }
      $scope.handleMessage = (msg) ->
        switch msg
          when "matched-player"
            $scope.startGame = true
            $scope.$apply()
          when "searching-game"
            $scope.startGame = false
          else
            console.log("unknown message")


]