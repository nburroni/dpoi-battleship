angular.module 'app'
.controller 'GameController', ['$scope', '$http',
  ($scope, $http) ->
    $scope.startGame = false
    $scope.placeShips = false
    $scope.letters = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J"]
    $scope.numbers = [0..9]
    $scope.selected = null
    $scope.opponentFires = []
    $scope.myFires = []
    $scope.currentShips = [{src: "assets/images/ships/ship-1.png", width: 2, id: 0},{src: "assets/images/ships/ship-1.png", width: 2, id: 1}]
    $scope.myBoard = []

    $scope.initBoard = ->
      for nico in [0..9]
        $scope.myBoard[nico] = []
        for lucho in [0..9]
          $scope.myBoard[nico][lucho] = []
    $scope.initBoard()
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
    $scope.getRelatives = (ship, id)->
      rels = []
      for idx in [1..(ship.width-1)]
        newid = id.substr(0, id.length-1)
        rel = document.getElementById(newid + (parseInt(id.charAt(id.length-1))+idx))
        if rel then rels.push rel
      rels
    $scope.checkRelatives = (ship, id) ->
      for idx in [(ship.width*-1)..(ship.width-1)]
        newid = id.substr(0, id.length-1)
        rel = document.getElementById(newid + (parseInt(id.charAt(id.length-1))+idx))
        if rel
          cellX = parseInt(id.charAt(id.length - 1))
          cellY = parseInt(id.charAt(id.length - 2))
          if $scope.myBoard[cellX][cellY].length != 0 then return false
      true
    $scope.onDragComplete = (data, evt) ->
      console.log("drag success, data:", data)
      console.log("evt success, evt:", evt)

    $scope.onDropComplete = (data, evt, that) ->
      td = document.getElementById(that)
      id = td.id.substr(td.id.length-2, td.id.length)
      theParent = evt.element[0].parentNode.getAttribute("class")
      target = $scope.myBoard[parseInt(id.charAt(1))][parseInt(id.charAt(0))]
      relatives = $scope.getRelatives(data, td.id);
      relativesFree = $scope.checkRelatives(data, td.id)
      if target.length == 0 and relativesFree
        if theParent.indexOf("ships-container") != -1
          $scope.currentShips = $scope.currentShips.filter((sh) -> sh.id != data.id)
          data.x = parseInt(id.charAt(1))
          data.y = parseInt(id.charAt(0))
          $scope.myBoard[parseInt(id.charAt(1))][parseInt(id.charAt(0))] = [data]
          relatives.forEach((cell) -> cell.style.opacity = 0)
        else
          prevId = td.id.substr(0, td.id.length-2) + data.y + data.x
          prevRelatives = $scope.getRelatives(data, prevId);
          prevRelatives.forEach((cell) -> cell.style.opacity = 1)
          $scope.myBoard[data.x][data.y] = []
          data.x = parseInt(id.charAt(1))
          data.y = parseInt(id.charAt(0))
          t$scope.myBoard[parseInt(id.charAt(1))][parseInt(id.charAt(0))] = [data]
      return ""

]