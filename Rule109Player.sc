Rule109Player {
	// builds elemCA using rule 109 and travese it such that when a loop of size repThresh is found,
	// window advances
	var <>ca, <>history, <>repThresh, <>mainRoutine;

	*new { |gridWidth, windowSize, repThresh,midiout, initial|
	^super.new.init(gridWidth, windowSize, repThresh, midiout, initial);
	}

	init { |gridWidth, windowSize, repThresh = 4, midiout, initial|
		this.ca = ElemCA(gridWidth, 109, initial, midiout);
		this.ca.setWindow(windowSize, 0, false);
		this.ca.windowVals();
		this.ca.window.postln;
		this.repThresh = repThresh;
		this.history = [this.ca.window];

	}

	traverse { |tempo = 1|
		// CALL THIS TO RUN
		var patternFeed;
		this.mainRoutine = Routine.new(
			{loop {
				patternFeed = this.ca.playNext();
				this.ca.patternFeedToEvent(patternFeed);
				this.history = this.history.add(this.ca.window().copy);
				this.searchForLoop();
				tempo.yield}
		}).play;
	}

	searchForLoop {
		// looks thru history to see if there is if there is periodicity for the last repThresh cycles
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
					{ loopSize = numHypo + 1; this.moveAndReset();}, // found it, jump to end of loop
					{ loopSize = loopSize + 1;} // no, try next hypothesis
				);
			});
	}

	moveAndReset {
		// found something
		this.history = [];
		this.ca.shiftWindow(1);
		if (this.ca.windowSize < 1, {this.mainRoutine.stop});
	}

}