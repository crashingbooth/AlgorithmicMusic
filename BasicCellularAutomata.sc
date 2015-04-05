BasicCA {
	/* abstract class for two state, 1D Cellular Automata.
	Subclasses will override methods related to rule creation and derivation.
	This superclass will handle conversion to patterns
	*/


	var <>width, <>prevState, <>nextState, <>started, <>history, <>midiout, <>windowSize, <>windowPos, <>window, <>scale, <>legato, <>tempo, <>cleanDisplay;


	*new {|width, firstState, midiout|
		^super.new.init(width, firstState, midiout)


	}

	init {|width, firstState, midiout|
		this.width = width;
		this.midiout = midiout;
		if (firstState == nil,
			{ this.nextState = this.randomGenerate() },
			{ this.nextState = firstState; this.width = this.nextState.size()}
		);
		this.prevState = "";
		this.nextState.do { this.prevState = this.prevState ++ "0" };
		this.started = false;
		this.history = [this.prevState];
		this.windowSize = 7;  // by default; set with this.setWindow
		this.windowPos = 0;
		this.window = "";
		this.cleanDisplay = false;
		this.legato = true;
		this.tempo = 1;

	}


	getNext {
		// abstract
	}

	displayHistory {
		this.history.do {|state, i|
			if (i >0 , {state.postln});
		};
	}

	randomGenerate {
		var res ="";
		this.width.do { res = res ++ 2.rand.asString };
		^res;
	}
	singleCellLine {
		var res = "";
		this.width.do { res = res ++ "0"};
		res = res[0..(width/2).asInteger] ++ "1"++ res[((width/2).asInteger +1)..];
		this.nextState = res.copy;
	}
	/*symmetricalRandomLine{
	}*/
	setWindow { |size, pos, center = true|
		if (size > this.width, {size = this.width });
		if (center,
			{ pos = ((this.width - size)/2).asInteger});
		// make sure everything fits, else trim window
		if ((pos + size) > this.width,
			{size = this.width - pos; "trimmed window".postln; });
		this.windowPos = pos;
		this.windowSize = size;
		this.started = false;

	}
	shiftWindow { |dist|
		var newStart, newEnd;
		newStart = this.windowPos + dist;
		newEnd = this.windowPos + this.windowSize + dist;
		if (newStart < 0, {newStart = 0});
		this.setWindow(this.windowSize, newStart, center:false);
	}

	displayCurrent {
		// called by playNext, shows complete state, with window bracketted off
		var outputString = "";
		this.nextState.do { |val, i|
			if (i == this.windowPos, {outputString = outputString ++ "["});
			if (this.cleanDisplay, {if (val.asString == "1", {val = "x"}, {val = " "});});
			outputString = outputString ++ val.asString;
			if (i == (this.windowPos + this.windowSize - 1), {outputString = outputString ++ "]"});
		};
		outputString.postln;

	}
	playNext {
		var patternFeed = [], pbs;
		this.windowVals();
		this.displayCurrent();
		this.windowSize.do { |i|
			if (this.nextState[this.windowPos + i].asString != this.prevState[this.windowPos + i].asString,
				{
					switch (this.nextState[this.windowPos + i].asString,
						"0", {patternFeed = patternFeed.add([i,\noteOff])},
						"1", {patternFeed = patternFeed.add([i,\noteOn])}
					)
			});

		this.getNext;
		^patternFeed;
		}

	}

	windowVals {
		var win;
		this.windowSize.do {|i| win = win ++ this.nextState[i + this.windowPos].asString};
		this.window = win.copy;
	}

	playThru { |tempo = 1|

		var r, patternFeed;
		this.tempo = tempo;
		r = Task.new({loop {
			this.patternFeedToEvent(this.playNext());
			this.tempo.wait} }).play;
	}
	patternFeedToEvent {|patternFeed|
			patternFeed.do {|feed, i|

			(	type:\midi,
				midiout: this.midiout,
				midicmd: feed[1],
				degree: feed[0],
				root: -6,
				scale: this.scale ).play;
		};
	}

}

CARules {
	var  <>rulesDict, <>ruleKeys, <>ruleNum;
	*new {|rules, ruleKeys|
		^super.new.init(rules, ruleKeys)

	}

	init {|rules, ruleKeys|

		this.ruleKeys = ruleKeys;
		this.rulesDict = this.createRules(rules);

	}

	createRules {|rules|
		var rulesDict = Dictionary.new;
		if (rules.isString,
			{ rulesDict = this.makeRulesFromString(rules)},
			{ rulesDict = this.makeRulesFromInt(rules)});
		^rulesDict;
	}

	makeRulesFromString { |rules|
		var rulesDict = Dictionary.new();
		if (rules.size != ruleKeys.size, { postf("rule string needs % ones or zeros", this.ruleKeys.size); ^nil; });
		rules.do { |rule, i|
			rule = rule.asString;
			if ((rule != "1") && (rule != "0"), {postln(["rules must contain only 0 and 1:", rule])});
			rulesDict.add(this.ruleKeys[i] -> rule);
		};
		this.setRuleNumFromString(rulesDict);
		^rulesDict;
	}

	setRuleNumFromString { |rulesDict|
		var reverseKeys, total = 0;
		reverseKeys = this.ruleKeys.reverse();
		reverseKeys.postln;
		reverseKeys.do { |key, i|
			if (key.isInteger != true, {key = key.asString});
			total = total + (rulesDict[key].asInteger*(2**i));
		};
		this.ruleNum = total;
	}
	makeRulesFromInt { |rules|
		var ruleArray, rulesDict = Dictionary.new();
		if (rules.isInteger != true, {"must be integer (or string)".postln; ^nil});
		if ((rules < 0) || (rules > (2**this.ruleKeys.size - 1)), {"rule number out of range".postln; ^nil});
		ruleArray = rules.asBinaryString;

		if (ruleArray.size < this.ruleKeys.size, {ruleArray = ruleArray.padLeft((this.ruleKeys.size) , string:"0")});

		if (ruleArray.size > this.ruleKeys.size, {ruleArray = ruleArray[(ruleArray.size - this.ruleKeys.size)..]});

		ruleArray.do { |rule, i|
			rulesDict.add(this.ruleKeys[i] -> rule.asString);
		};
		this.ruleNum = rules;
		^rulesDict;

	}
	showRules {
		this.ruleKeys.do {|key| [key, this.rulesDict[key]].postln};

	}

}
/* TODO
- create re-init, for when resetting stuff
- gen single cell, gen symetrical random (include this in sublcass args?)
*/
