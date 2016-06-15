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
    $scope.currentShips = [{src: "assets/images/ships/ship-1.png", width: 2, id: 0, height: 1},{src: "assets/images/ships/ship-1.png", width: 3, id: 1, height: 1}]
    $scope.myBoard = []

    $scope.shipsPlaced = ->
      sendableShips = []
      for i in [0..9]
        for j in [0..9]
          currentImg = $scope.myBoard[i][j].img
          if currentImg.length != 0
            shp = currentImg[0]
            sendableShips.push {start: {x: shp.x, y: shp.y}, end: {x: shp.endX, y: shp.endY}}
      console.log sendableShips
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
        when "game-ready"
          $scope.placeShips = false
        else
          console.log("unknown message")
      $scope.$apply()

    $scope.getHRelatives = (ship, id)->
      rels = []
      for idx in [1..(ship.width-1)]
        newid = id.substr(0, id.length-1)
        rel = document.getElementById(newid + (parseInt(id.charAt(id.length-1))+idx))
        if rel then rels.push rel
      rels
    $scope.getVRelatives = (ship, id)->
      rels = []
      x = parseInt id.charAt(id.length-1)
      for idx in [1..(ship.height-1)]
        newid = id.substr(0, id.length-2)
        relId = newid + (parseInt(id.charAt(id.length-2))+idx) + x
        rel = document.getElementById(relId)
        if rel then rels.push rel
      rels

    $scope.checkHRelatives = (ship, id) ->
      y = parseInt id.substr 0, id.length-1
      x = parseInt id.substr id.length - 1, id.length
      for i in [0..ship.width-1]
        return true if $scope.myBoard[x+i][y].busy
      false
    $scope.checkVRelatives = (ship, id) ->
      y = parseInt id.substr 0, id.length-1
      x = parseInt id.substr id.length - 1, id.length
      for i in [0..ship.height-1]
        return true if $scope.myBoard[x][y+i].busy
      false


    $scope.rotateShip = (sp, id)->
      if sp.height == 1
        sp.height = sp.width
        $scope.myBoard[sp.x][sp.y].busy = false
        relativesFree = $scope.checkVRelatives(sp, id.substr(id.length-2, id.length))
        $scope.myBoard[sp.x][sp.y].busy = true
        sp.height = 1
        if not relativesFree
          prevId = id.substr(0, id.length-2) + sp.y + sp.x
          prevRelatives = $scope.getHRelatives(sp, prevId);
          sp.height = sp.width
          sp.width = 1
          sp.src = sp.src.replace("ship-", "ship-r-")
          prevRelatives.forEach((cell) -> cell.style.opacity = 1)
          for j in [0..sp.height-1]
            $scope.myBoard[sp.x+j][sp.y].busy = false
          for k in [0..sp.height-1]
            $scope.myBoard[sp.x][sp.y+k].busy = true
          relatives = $scope.getVRelatives(sp, id)
          relatives.forEach((cell) -> cell.style.opacity = 0)
      else
        sp.width = sp.height
        $scope.myBoard[sp.x][sp.y].busy = false
        relativesFree = $scope.checkHRelatives(sp, id.substr(id.length-2, id.length))
        $scope.myBoard[sp.x][sp.y].busy = true
        sp.width = 1
        if not relativesFree
          prevId = id.substr(0, id.length-2) + sp.y + sp.x
          prevRelatives = $scope.getVRelatives(sp, prevId);
          sp.width = sp.height
          sp.height = 1
          sp.src = sp.src.replace("ship-r-", "ship-")
          prevRelatives.forEach((cell) -> cell.style.opacity = 1)
          for j in [0..sp.width-1]
            $scope.myBoard[sp.x][sp.y+j].busy = false
          for k in [0..sp.width-1]
            $scope.myBoard[sp.x+k][sp.y].busy = true
          relatives = $scope.getHRelatives(sp, id)
          relatives.forEach((cell) -> cell.style.opacity = 0)

#    $scope.rotatingButton = (->
#      button = document.createElement("img")
#      button.classList.add("rotate-button")
#      button.onclick = $scope.rotateShip
#      button.src = "assets/images/rotate.jpg"
#      button.id = "rotate"
#      button
#    )()


    $scope.onDragComplete = (data, evt) ->
      for j in [0..data.width-1]
        $scope.myBoard[data.x+j][data.y].busy = false

    $scope.rotateButton = (sp, boolean) ->
      td = document.getElementById "ship-"+sp.y+""+sp.x
      if boolean
        td.appendChild($scope.rotatingButton)
      else
        document.getElementById("rotate").remove()
      return

    $scope.onDropComplete = (data, evt, that) ->
      td = document.getElementById(that)
      id = td.id.substr(td.id.length-2, td.id.length)
      theParent = evt.element[0].parentNode.getAttribute("class")
      if data.height == 1
        relatives = $scope.getHRelatives(data, td.id);
        relativesFree = $scope.checkHRelatives(data, id)
        if not relativesFree
          if theParent.indexOf("ships-container") != -1
            $scope.currentShips = $scope.currentShips.filter((sh) -> sh.id != data.id)
            data.x = parseInt(id.charAt(1))
            data.y = parseInt(id.charAt(0))
            $scope.myBoard[parseInt(id.charAt(1))][parseInt(id.charAt(0))].img = [data]
            for i in [0..data.width-1]
              $scope.myBoard[parseInt(id.charAt(1))+i][parseInt(id.charAt(0))].busy = true
            data.endX = data.x + data.width - 1
            data.endY = data.y + data.height - 1
            relatives.forEach((cell) -> cell.style.opacity = 0)
          else
            prevId = td.id.substr(0, td.id.length-2) + data.y + data.x
            prevRelatives = $scope.getHRelatives(data, prevId);
            prevRelatives.forEach((cell) -> cell.style.opacity = 1)
            for j in [0..data.width-1]
              $scope.myBoard[data.x+j][data.y].busy = false
            $scope.myBoard[data.x][data.y].img = []
            data.x = parseInt(id.charAt(1))
            data.y = parseInt(id.charAt(0))
            $scope.myBoard[parseInt(id.charAt(1))][parseInt(id.charAt(0))].img = [data]
            for j in [0..data.width-1]
              $scope.myBoard[parseInt(id.charAt(1))+j][parseInt(id.charAt(0))].busy = true
            data.endX = data.x + data.width - 1
            data.endY = data.y
            relatives.forEach((cell) -> cell.style.opacity = 0)
        else
          for j in [0..data.width-1]
            $scope.myBoard[data.x+j][data.y].busy = true
          ""
      else
        relatives = $scope.getVRelatives(data, td.id);
        relativesFree = $scope.checkVRelatives(data, id)
        if not relativesFree
          if theParent.indexOf("ships-container") != -1
            $scope.currentShips = $scope.currentShips.filter((sh) -> sh.id != data.id)
            data.x = parseInt(id.charAt(1))
            data.y = parseInt(id.charAt(0))
            $scope.myBoard[parseInt(id.charAt(1))][parseInt(id.charAt(0))].img = [data]
            for i in [0..data.height-1]
              $scope.myBoard[parseInt(id.charAt(1))][parseInt(id.charAt(0))+i].busy = true
            data.endX = data.x + data.width - 1
            data.endY = data.y + data.height - 1
            relatives.forEach((cell) -> cell.style.opacity = 0)
          else
            prevId = td.id.substr(0, td.id.length-2) + data.y + data.x
            prevRelatives = $scope.getVRelatives(data, prevId);
            prevRelatives.forEach((cell) -> cell.style.opacity = 1)
            for j in [0..data.height-1]
              $scope.myBoard[data.x][data.y+j].busy = false
            $scope.myBoard[data.x][data.y].img = []
            data.x = parseInt(id.charAt(1))
            data.y = parseInt(id.charAt(0))
            $scope.myBoard[parseInt(id.charAt(1))][parseInt(id.charAt(0))].img = [data]
            for j in [0..data.height-1]
              $scope.myBoard[parseInt(id.charAt(1))][parseInt(id.charAt(0))+j].busy = true
            data.endX = data.x + data.width - 1
            data.endY = data.y + data.height - 1
            relatives.forEach((cell) -> cell.style.opacity = 0)
        else
          for j in [0..data.height-1]
            $scope.myBoard[data.x][data.y+j].busy = true
          ""

]