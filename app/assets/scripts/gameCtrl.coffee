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
    $scope.currentShips = [{src: "assets/images/ships/ship-1.png", width: 2, id: 0, height: 1},{src: "assets/images/ships/ship-1.png", width: 3, id: 1, height: 1}, {src: "assets/images/ships/ship-1.png", width: 3, id: 2, height: 1}, {src: "assets/images/ships/ship-1.png", width: 4, id: 3, height: 1}, {src: "assets/images/ships/ship-1.png", width: 5, id: 4, height: 1}]
#    $scope.currentShips = [{src: "assets/images/ships/ship-1.png", width: 2, id: 0, height: 1}]
    $scope.myBoard = []
    $scope.fireMessage = {}
#    $scope.hitMessage = {message: "HIT", icon: "fa fa-dot-circle-o"}
    $scope.hitMessage = {message: "HIT", gif: "assets/images/hit.gif"}
#    $scope.sinkMessage = {message: "SUNKEN", icon: "fa fa-anchor"}
    $scope.sinkMessage = {message: "SUNKEN", gif: "assets/images/sunken.gif"}
#    $scope.missMessage = {message: "MISS", icon: "fa fa-tint"}
    $scope.missMessage = {message: "MISS", gif: "assets/images/water.gif"}
    $scope.searching = false
    $scope.result = {show: false, message:""}
    $scope.hitImg = {src: "assets/images/fire.png"}
    $scope.missImg = {src: "assets/images/water.png"}
    $scope.hits = 0
    $scope.misses = 0
    $scope.oHits = 0
    $scope.oMisses = 0
    $scope.startedTime = 0
    $scope.rival = {}

    window.testStatistics = ->
      socket.send {action: "save-data", matchData:{
        won: true
        hits: 17
        misses: 5
        time: (1000*60*10)
      }}
    $scope.shipsPlaced = ->
      $('#waiting-modal').modal('show')
      sendableShips = []
      for i in [0..9]
        for j in [0..9]
          currentImg = $scope.myBoard[i][j].img
          if currentImg.length != 0
            shp = currentImg[0]
            if shp.width == 1
              lives = shp.height
              relatives = $scope.getVRelatives(shp, 'my-'+ shp.y + shp.x)
              document.getElementById("my-")
#              relatives.forEach((cell) -> cell.style.opacity = 0)
            else
              lives = shp.width
              relatives = $scope.getHRelatives(shp, 'my-'+ shp.y + shp.x)
#              relatives.forEach((cell) -> cell.style.opacity = 0)
            sendableShips.push {start: {x: shp.x, y: shp.y}, end: {x: shp.endX, y: shp.endY}, lives: lives}

      console.log sendableShips
      socket.send {action: "placed-ships", ships: sendableShips}
      $scope.placeShips = false


    $scope.initBoard = ->
      for nico in [0..9]
        $scope.myBoard[nico] = []
        for lucho in [0..9]
          $scope.myBoard[nico][lucho] = {img:[], busy: false, feedback: []}

    if $scope.myBoard.length == 0 then $scope.initBoard()

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
    $scope.searchGame = ->
      $scope.searching = true
      socket.send {action: "search-game"}
    socket.onopen ->
      socket.send {action: "reconnect"}
      $('#loading-modal').modal('toggle')
    socket.connect()
    showModal = true
    window.onbeforeunload = () ->
      socket.send {action: "save-player"}
    getShipLength = (ship) ->
      if ship.start.x != ship.end.x then return {length: ship.end.x - ship.start.x + 1, or: 'h'} else return {length: ship.end.y - ship.start.y + 1, or: 'v'}
    $scope.handleMessage = (response) ->
      switch response.msg
        when "reconnected"
          $('#loading-modal').modal('hide')
          console.log(response)
          newData = response.data
          $scope.myTurn = newData.turn
          showModal = false
          currId = 0
          if $scope.myBoard.length == 0 then $scope.initBoard()
          for ship in newData.ships
            shipInfo = getShipLength(ship)
            if shipInfo.or == 'h'
              $scope.myBoard[ship.start.x][ship.start.y].img.push {src: "assets/images/ships/ship-1.png", width: shipInfo.length, id: currId, height: 1}
              for i in [ship.start.x..ship.end.x]
                $scope.myBoard[i][ship.start.y].busy = true
            else
              $scope.myBoard[ship.start.x][ship.start.y].img.push {src: "assets/images/ships/ship-r-1.png", width: 1, id: currId, height: shipInfo.length}
              for j in [ship.start.y..ship.end.y]
                $scope.myBoard[ship.start.x][j].busy = true
            currId++
          for fire in newData.myFires
            if fire.hit
              $scope.handleMessage {msg: "hit", fire: fire.coords}
            else
              $scope.handleMessage {msg: "miss", fire: fire.coords}
          for oppFire in newData.oppFires
            if oppFire.hit
              $scope.handleMessage {msg: "hit-received", fire: oppFire.coords}
            else
              $scope.handleMessage {msg: "miss-received", fire: oppFire.coords}
          $scope.currentShips = []
          $scope.searching = false
          $scope.selected = null
          $scope.placeShips = false
          $scope.startGame = true
          showModal = true
        when "not-reconnected" then $('#loading-modal').modal('hide')
        when "matched-player"
          $scope.searching = false
          $scope.startGame = true
          $scope.placeShips = true
          $scope.startedTime = new Date().getTime()
          response.oppId = -1 if !response.oppId
          FB.api "/#{response.oppId}/picture", (response) ->
            if response && !response.error
              $scope.rival.imageSrc = response.data.url
              $scope.$apply()
          $http.get "/user/#{response.oppId}/name"
            .success (response) -> $scope.rival.name = response
        when "searching-game"
          $scope.startGame = false
          $scope.placeShips = false
        when "hit-received"
          $scope.oHits++
          $scope.opponentFires.push(response.fire)
          $("#my-"+response.fire.y+''+response.fire.x).addClass("hit-target")
          $scope.myBoard[response.fire.x][response.fire.y].feedback = [$scope.hitImg]
          $scope.fireMessage = $scope.hitMessage
          if showModal
            $('#fire-modal').modal('toggle')
            setTimeout(->
              $('#fire-modal').modal('hide')
            , 2000)
        when "sunk-received"
          $scope.oHits++
          $scope.opponentFires.push(response.fire)
          $("#my-"+response.fire.y+''+response.fire.x).addClass("hit-target")
          $scope.myBoard[response.fire.x][response.fire.y].feedback = [$scope.hitImg]
          $scope.fireMessage = $scope.sinkMessage
          $('#fire-modal').modal('toggle')
          setTimeout(->
            $('#fire-modal').modal('hide')
          , 5500)

        when "miss-received"
          $scope.oMisses++
          $scope.opponentFires.push(response.fire)
          $("#my-"+response.fire.y+''+response.fire.x).addClass("miss-target")
          $scope.myBoard[response.fire.x][response.fire.y].feedback = [$scope.missImg]
          $scope.fireMessage = $scope.missMessage
          if showModal
            $('#fire-modal').modal('toggle')
            setTimeout(->
              $('#fire-modal').modal('hide')
            , 2000)
        when "hit"
          $scope.hits++
          $scope.selected = null
          $scope.myFires.push(response.fire)
          $("#opp-"+response.fire.y+''+response.fire.x).removeClass()
          $("#opp-"+response.fire.y+''+response.fire.x).addClass("hit-target")
          $scope.fireMessage = $scope.hitMessage
          if showModal
            $('#fire-modal').modal('toggle')
            setTimeout(->
              $('#fire-modal').modal('hide')
            , 2000)
        when "miss"
          $scope.misses++
          $scope.selected = null
          $scope.myFires.push(response.fire)
          $("#opp-"+response.fire.y+''+response.fire.x).removeClass()
          $("#opp-"+response.fire.y+''+response.fire.x).addClass("miss-target")
          $scope.fireMessage = $scope.missMessage
          if showModal
            $('#fire-modal').modal('toggle')
            setTimeout(->
              $('#fire-modal').modal('hide')
            , 2000)
        when "my-turn"
          $scope.myTurn = true
        when "their-turn"
          $scope.myTurn = false
        when "game-ready"
          $scope.placeShips = false
          $('#waiting-modal').modal('hide')
        when "sunk-ship"
          $scope.hits++
          $scope.selected = null
          $scope.myFires.push(response.fire)
          $("#opp-"+response.fire.y+''+response.fire.x).removeClass()
          $("#opp-"+response.fire.y+''+response.fire.x).addClass("hit-target")
          $scope.fireMessage = $scope.sinkMessage
          $('#fire-modal').modal('toggle')
          setTimeout(->
            $('#fire-modal').modal('hide')
          , 5500)
        when "won-match"
          response.msg = "sunk-ship"
          $scope.handleMessage(response)
          setTimeout(->
            $scope.result = {show: true, message: "You won :) !"}
            socket.send {action: "save-data", matchData:{
              won: true
              hits: $scope.hits
              misses: $scope.misses
              time: new Date().getTime() - $scope.startedTime
            }}
            $scope.$apply()
          ,5600)
        when "lost-match"
          response.msg = "sunk-received"
          $scope.handleMessage(response)
          setTimeout(->
            $scope.result = {show: true, message: "You lost :'( !"}
            socket.send()
            $scope.$apply()
          ,5600)
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
        return true if (x+i >9) || (not $scope.myBoard[x+i][y]) || ($scope.myBoard[x+i][y].busy)
      false
    $scope.checkVRelatives = (ship, id) ->
      y = parseInt id.substr 0, id.length-1
      x = parseInt id.substr id.length - 1, id.length
      for i in [0..ship.height-1]
        return true if (y+i >9) || (not $scope.myBoard[x][y+i]) || ($scope.myBoard[x][y+i].busy)
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
          sp.endX = sp.x
          sp.endY = sp.y + sp.height - 1
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
          sp.endX = sp.x + sp.width - 1
          sp.endY = sp.y

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
        return
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
        return

]