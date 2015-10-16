Rule109ManualPlayer : Rule109Player {
	var <>loopDetected = false, <>nextPos = 0, <> nextAction, <>loopPos, <>loopSize, <>automatic = false;
	/*
	*new { |creationArgs, moreArgs|
		^super.new(creationArgs).custom_init(moreArgs); }
	custom_init { |moreArgs|
		this.addionalInstanceVars = moreArgs; }*/
	searchForLoop {
		// looks thru history to see if there is if there is periodicity for the last repThresh cycles
		// overrides parent class
		var numHypo = (this.history.size / this.repThresh).asInteger, loopSize = 1;
		// outerloop: test each loopsize
		while ( { loopSize <= numHypo },
			{
				var isValid = true, el = 0;
				// middle loop: looking at each element in loop
				while ( { (el < loopSize) && isValid },
					{	var testAgainst = this.history[this.history.size - 1 - el], step = 1;
						// inner loop: checking the validity of each element in loop against repThresh repetitions
						while ( { (step < this.repThresh)  && isValid },
							{
								if ( testAgainst != this.history[this.history.size - 1 - el - (step * loopSize)],
									{ isValid = false }, // nope, should go to outerloop and increment loopSize
									{ step = step + 1} // sfsg
								);
							}
						);
						el = el + 1;
					});
				if ( isValid,
					{ loopSize = numHypo + 1; this.uponDetection();}, // found it, jump to end of loop
					{ loopSize = loopSize + 1;} // no, try next hypothesis
				);
			});
	}

	uponDetection {
		["history size", this.history.size].postln;
		if (this.nextPos != 0, {
			this.changePos();
		});
		this.loopDetected = true;
		this.loopSize = (this.history.size / 2).asInteger;
		this.loopPos = 0;

		if (this.ca.windowSize < 1, {this.mainRoutine.stop});
	}
	traverse { |tempo = 1|
		// CALL THIS TO RUN
		var patternFeed;
		this.mainRoutine = Routine.new(
			{loop {
				patternFeed = this.ca.playNext();
				this.ca.patternFeedToEvent(patternFeed);
				this.history = this.history.add(this.ca.window().copy);
				if (this.loopDetected == false, {
					this.searchForLoop();}, {this.checkIfReady()});
				tempo.yield}
		}).play;
	}
	checkIfReady {
		this.loopPos = this.loopPos + 1;
		if (this.nextPos != 0, {
			if ((this.loopPos % this.loopSize) == 0, {this.changePos()})});

	}
	changePos {
		this.history = [];
		this.loopDetected = false;
		this.loopSize = nil;
		this.loopPos = 0;
		this.ca.shiftWindow(this.nextPos);
		if (this.automatic == false, { this.nextPos = 0 });
	}
	start {}
	goLeftNow {|distance = 1|
		this.nextPos = (-1 * distance);
		this.changePos() }
	goLeftAtNext { |distance = 1|
		this.nextPos = (-1 * distance)}
	goRightAtNext {|distance = 1|
		this.nextPos = distance}
	goRightNow {|distance = 1|
		this.nextPos = distance; this.changePos() }
	expandWindow {
		var current;
		current = this.markSettings;
		this.ca.setWindow(current[0] + 1, current[1], false);
	}
	shrinkWindow { var current;
		current = this.markSettings;
		this.ca.setWindow(current[0] - 1, current[1], false);}
	markSettings {
		^[this.ca.windowSize, this.ca.windowPos]}
	goToPosNow { |markedSetting|
		this.ca.setWindow(markedSetting[0], markedSetting[1], false)}
	regenerate {
		this.ca.nextState = this.ca.randomGenerate()}
	goToPosAtNext {}
	end {}

}