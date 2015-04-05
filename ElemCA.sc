ElemCA : BasicCA {

	var <>rules, <>keys, <>ruleNum;

	*new {|width, rules, firstState, midiout|
		^super.new.el_init(width, rules, firstState,midiout)

	}

	el_init {|width, rules, firstState, midiout|
		super.init(width, firstState,midiout);
		this.keys = ["111","110","101","100","011","010","001","000"];
		this.rules = CARules(rules, this.keys);
	}


	getNext {
		var nextGen;
		this.prevState = this.nextState.copy;
				this.history = this.history.add(this.prevState);
		this.prevState.do { |state, i|
			var neighbours =  this.prevState.wrapAt(i-1).asString ++ state.asString ++ this.prevState.wrapAt(i+1).asString;
			nextGen = nextGen ++ this.rules.rulesDict[neighbours];
		};
		this.nextState = nextGen.copy;
		^nextGen;
	}
}