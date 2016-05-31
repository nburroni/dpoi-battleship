angular.module 'app', []
.controller 'FbController', ['$scope', '$http',
  ($scope, $http) ->

    window.fbAsyncInit = ->
      FB.init
        appId      : '1550248965271409'
        cookie     : true
        xfbml      : true
        version    : 'v2.5'

      FB.getLoginStatus (response) -> $scope.checkStatus(response)

    window.fbLogin = ->
      document.getElementById('status').innerHTML = "Logging in."
      FB.getLoginStatus((response) ->
        $scope.checkStatus(response)
      )

    $scope.checkStatus = (response) ->
      if response.status == 'connected'
        $scope.logged()
      else if (response.status == 'not_authorized')
        document.getElementById('status').innerHTML = 'Please log ' + 'into this app.'
      else
        document.getElementById('status').innerHTML = 'Please log ' +  'into Facebook.'

    $scope.logged = ->
      FB.api '/me', (user) ->
        data = { name: user.name, id: user.id }
        $http.post("/fb-login", data)
          .success (message) ->
            console.log(message)
          .error (message) ->
            console.log(message)

]

