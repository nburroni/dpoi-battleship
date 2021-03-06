angular.module 'app'
.controller 'FbController', ['$scope', '$http',
  ($scope, $http) ->

    window.fbAsyncInit = ->
      FB.init
        appId      : '1550248965271409'
        cookie     : true
        xfbml      : true
        version    : 'v2.5'

      FB.getLoginStatus (response) -> $scope.checkStatus(response)

    $scope.login = ->
      FB.login ((response) -> $scope.checkStatus(response)), { scope: 'public_profile,email' }

    window.fbLogin = ->
      document.getElementById('status').innerHTML = "Logging in."
      FB.getLoginStatus((response) ->
        $scope.checkStatus(response)
      )

    $scope.checkStatus = (response) ->
      if response.status == 'connected'
        $scope.logged()
      else if (response.status == 'not_authorized')
        console.log 'Please log into this app.'
      else
        console.log  'Please log into Facebook.'

    $scope.logged = ->
      FB.api '/me', (user) ->
        data = { name: user.name, id: user.id }
        $http.post("/fb-login", data)
          .success (message) -> window.location.href = "/game"
          .error (message) -> console.log(message)

]

