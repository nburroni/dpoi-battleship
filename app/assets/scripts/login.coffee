angular.module 'app', []
.controller 'FbController', ['$scope', '$http',
  ($scope, $http) ->
    window.fbLogin = ->
      document.getElementById('status').innerHTML = "Logging in."
      FB.getLoginStatus((response) ->
        $scope.checkStatus(response)
      )
    $scope.checkStatus = (response) ->
      if response.status == 'connected'
        $scope.logged(response)
      else if (response.status == 'not_authorized')
        document.getElementById('status').innerHTML = 'Please log ' + 'into this app.'
      else
        document.getElementById('status').innerHTML = 'Please log ' +  'into Facebook.'
    $scope.logged = (response) ->
      data = {name: response.name, id: response.id}
      $http.post("/fb-login", data)
      .success((message)->
        console.log(message)
      ).error((message)->
        console.log(message)
      )
]

