angular.module 'app'
.controller 'GameController', ['$scope', '$http',
  ($scope, $http) ->
    $scope.startGame = false
    $scope.placeShips = false
    $scope.letters = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J"]
    $scope.numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
    $scope.selected = null
    $scope.opponentFires = []
    $scope.myFires = []

    $scope.selectPosition = (x, y) ->
      if $scope.selected
        $("#opp-"+$scope.selected.y+''+$scope.selected.x).removeClass("selected-target")
      $scope.selected = {
        x: x
        y: y
      }
      $("#opp-"+y+''+x).addClass("selected-target")
      return
    $scope.fire = ->
      $scope.myFires.push($scope.selected)
      socket.send {action: "fire", fire: $scope.selected}
    socket = window.socketUtil

    socket.onmessage (data) -> $scope.handleMessage(data)
    socket.connect()
    socket.onopen ->
      socket.send {action: "search-game"}
    $scope.handleMessage = (response) ->
      switch response.msg
        when "matched-player"
          $scope.startGame = true
          $scope.placeShips = true
        when "searching-game"
          $scope.startGame = false
          $scope.placeShips = false
        when "hit-received"
          $scope.opponentFires.push(response.fire)
          $("#my-"+response.fire.y+''+response.fire.x).addClass("hit-target")
        when "miss-received"
          $scope.opponentFires.push(response.fire)
          $("#my-"+response.fire.y+''+response.fire.x).addClass("miss-target")
        when "hit"
          $scope.selected = null
          $scope.myFires.push(response.fire)
          $("#opp-"+response.fire.y+''+response.fire.x).removeClass()
          $("#opp-"+response.fire.y+''+response.fire.x).addClass("hit-target")
        when "miss"
          $scope.selected = null
          $scope.myFires.push(response.fire)
          $("#opp-"+response.fire.y+''+response.fire.x).removeClass()
          $("#opp-"+response.fire.y+''+response.fire.x).addClass("miss-target")
        else
          console.log("unknown message")
      $scope.$apply()


]