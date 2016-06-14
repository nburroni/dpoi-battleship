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
    $scope.currentShips = [{src: "assets/images/ships/ship-1.png", width: 2, id: 0},{src: "assets/images/ships/ship-1.png", width: 3, id: 1}]
    $scope.myBoard = []
    $scope.shipsPlaced = ->
      $scope.placeShips = false
      sendableShips = []
      for i in [0..9]
        for j in [0..9]
          currentImg = $scope.myBoard[i][j].img
          if currentImg.length != 0
            shp = currentImg[0]
            sendableShips.push {start: {x: shp.x, y: shp.y}, end: {x: shp.endX, y: shp.endY}}
      socket.send {action: "placed-ships", ships: sendableShips}
    $scope.initBoard = ->
      for nico in [0..9]
        $scope.myBoard[nico] = []
        for lucho in [0..9]
          $scope.myBoard[nico][lucho] = {img:[], busy: false}
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
        when "my-turn"
          $scope.myTurn = true
        when "their-turn"
          $scope.myTurn = false
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
      y = parseInt id.substr 0, id.length-1
      x = parseInt id.substr id.length - 1, id.length
      for i in [0..ship.width-1]
        return true if $scope.myBoard[x+i][y].busy
      false

#    $scope.onDragStart = (data, evt) ->

    $scope.onDragComplete = (data, evt) ->
      for j in [0..data.width-1]
        $scope.myBoard[data.x+j][data.y].busy = false

    $scope.onDropComplete = (data, evt, that) ->
      td = document.getElementById(that)
      id = td.id.substr(td.id.length-2, td.id.length)
      theParent = evt.element[0].parentNode.getAttribute("class")
      relatives = $scope.getRelatives(data, td.id);
      relativesFree = $scope.checkRelatives(data, id)
      if not relativesFree
        if theParent.indexOf("ships-container") != -1
          $scope.currentShips = $scope.currentShips.filter((sh) -> sh.id != data.id)
          data.x = parseInt(id.charAt(1))
          data.y = parseInt(id.charAt(0))
          $scope.myBoard[parseInt(id.charAt(1))][parseInt(id.charAt(0))].img = [data]
          for i in [0..data.width-1]
            $scope.myBoard[parseInt(id.charAt(1))+i][parseInt(id.charAt(0))].busy = true
          data.endX = i-1
          data.endY = data.y
          relatives.forEach((cell) -> cell.style.opacity = 0)
        else
          prevId = td.id.substr(0, td.id.length-2) + data.y + data.x
          prevRelatives = $scope.getRelatives(data, prevId);
          prevRelatives.forEach((cell) -> cell.style.opacity = 1)
          for j in [0..data.width-1]
            $scope.myBoard[data.x+j][data.y].busy = false
          $scope.myBoard[data.x][data.y].img = []
          data.x = parseInt(id.charAt(1))
          data.y = parseInt(id.charAt(0))
          $scope.myBoard[parseInt(id.charAt(1))][parseInt(id.charAt(0))].img = [data]
          for j in [0..data.width-1]
            $scope.myBoard[parseInt(id.charAt(1))+j][parseInt(id.charAt(0))].busy = true
          data.endX = j-1
          data.endY = data.y
          relatives.forEach((cell) -> cell.style.opacity = 0)
      else
        for j in [0..data.width-1]
          $scope.myBoard[data.x+j][data.y].busy = true
        ""

]