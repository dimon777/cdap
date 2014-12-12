/**
 * Login Ctrl
 */

angular.module(PKG.name+'.feature.login').controller('LoginCtrl',
function ($scope, myAuth, $alert, $state, cfpLoadingBar, $timeout, MYAUTH_EVENT, MY_CONFIG, caskFocusManager) {

  $scope.credentials = myAuth.remembered();

  $scope.submitting = false;

  $scope.doLogin = function (c) {
    $scope.submitting = true;
    cfpLoadingBar.start();

    myAuth.login(c)
      .finally(function(){
        $scope.submitting = false;
        cfpLoadingBar.complete();
        $alert({
          title:'Welcome!',
          content:'You\'re logged in!',
          type:'success'
        });
      });
  };

  $scope.$on('$viewContentLoaded', function() {
    if(myAuth.isAuthenticated()) {
      $alert({
        content: 'You are already logged in!',
        type: 'warning'
      });
      $state.go('home');
    }
    else {

      if(MY_CONFIG.securityEnabled) {
        focusLoginField();
      }
      else { // auto-login
        myAuth.login({username:'admin', password:'admin'});
      }

    }
  });

  $scope.$on(MYAUTH_EVENT.loginFailed, focusLoginField);

  /* ----------------------------------------------------------------------- */

  function focusLoginField() {
    $timeout(function() {
      caskFocusManager.select($scope.credentials.username ? 'password' : 'username');
    }, 10); // the addtl timeout is so this triggers AFTER any potential focus() on an $alert
  }

});





