kerubApp.controller('ExpectationMini', function($scope, $log, size) {
	$scope.icon = 'cog';
	$scope.description = '';
	$scope.style = 'info';

	var expLevelToStyle = function(expectationLevel) {
		switch(expectationLevel) {
			case 'Hint':
				return 'success';
			case 'Want':
				return 'info';
			case 'DealBreaker':
				return 'warning';
			default:
				$log.error('Unknown expectation level', expectationLevel);
				return 'error';
		}
	};

	var expToIcon = function(expectation) {
		switch(expectation['@type']) {
			case 'availability':
				return 'fa fa-power-off';
			case 'storage-redundancy':
				return 'glyphicon glyphicon-hdd';
			case 'storage-read-perf':
				return 'glyphicon glyphicon-arrow-up';
			case 'storage-write-perf':
				return 'glyphicon glyphicon-arrow-down'
			case 'cpu-clock-freq':
				return 'glyphicon glyphicon-flash';
			case 'ram-clock-freq':
				return 'glyphicon glyphicon-dashboard';
			case 'cache-size':
				return 'glyphicon glyphicon-plus-sign';
			default:
				$log.warn('Unknown expectation type', expectation['@type']);
				return 'cog';
		}
	};

	var expToDesc = function(expectation) {
		switch(expectation['@type']) {
			case 'storage-redundancy':
				return expectation.nrOfCopies;
			case 'storage-read-perf':
				return size.humanFriendlySize(expectation.speed.kbPerSec * 1024) + '/s';
			case 'cache-size':
				return size.humanFriendlySize(expectation.minKbytes * 1024) + '+'
			case 'ram-clock-freq':
				return expectation.min + ' Mhz +'
			case 'cpu-clock-freq':
				return expectation.minimalClockFrequency + ' Mhz +'
			case 'availability':
				return expectation.up ? 'on' : 'off';
			default:
				$log.warn('Unknown expectation type', expectation['@type']);
            	return '?';
		}
	};

	$scope.init = function(expectation) {
		$scope.style = expLevelToStyle(expectation.level)
		$scope.icon = expToIcon(expectation);
		$scope.description = expToDesc(expectation);
	}
});