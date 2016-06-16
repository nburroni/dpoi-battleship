angular.module 'app'
.controller 'ProfileController', ['$scope', '$http',
  ($scope, $http) ->

    FB.init
      appId      : '1550248965271409'
      cookie     : true
      xfbml      : true
      version    : 'v2.5'

    $scope.user = {}

    $scope.fetchLoggedUser = ->
      FB.getLoginStatus (response) ->
#        window.location.href = '/' if response.status != 'connected'
        FB.api '/me', (user) ->
          $scope.user = { name: user.name, id: user.id }
          FB.api "/#{user.id}/picture", (response) ->
            if response && !response.error
              $scope.user.imageSrc = response.data.url
              $scope.$apply()



    $scope.fetchLoggedUser()
]

