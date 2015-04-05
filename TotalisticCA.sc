TotalisticCA : BasicCA {

	var <>rules, <>keys, <>ruleNum, <>numNeighbours, <>weights, <>isWeighted;

	*new {|width, rules, firstState, midiout, numNeighbours, isWeighted|
		^super.new.my_init(width, rules, firstState,midiout, numNeighbours, isWeighted)

	}

	my_init {|width, rules, firstState, midiout, numNeighbours = 2, isWeighted = true|
		super.init(width, firstState,midiout);

		this.numNeighbours = numNeighbours;
		this.isWeighted = isWeighted;

		if (this.isWeighted == true,
			{this.weights = (1..(this.numNeighbours + 1))},
			{this.weights = Array.fill(this.numNeighbours + 1, { 1 });});

		this.keys = (Array.fill((TotalisticCA.calcNumRules(this.weights)), {|i| i })).reverse;

		this.rules = CARules(rules, this.keys);
	}

	*calcNumRules { |weights|
		var total = 1; //start with possibility of all zeros
		weights.do  {|w|
			total = total + (2*w);
		};
		total = total - weights.wrapAt(-1);
		^total;
	}
	getNext {
		var nextGen;
		this.prevState = this.nextState.copy;
		this.history = this.history.add(this.prevState);
		this.prevState.do { |state, i|
			var neighbourhoodVal;
			neighbourhoodVal = (state.asString.asInteger * this.weights[this.numNeighbours]);
			(1..this.numNeighbours).do { |neighbour|
				neighbourhoodVal = neighbourhoodVal + (this.prevState.wrapAt(i-neighbour).asString.asInteger * this.weights[this.numNeighbours - neighbour]);
				neighbourhoodVal = neighbourhoodVal + (this.prevState.wrapAt(i+neighbour).asString.asInteger * this.weights[this.numNeighbours - neighbour]);

			};

			nextGen = nextGen ++ this.rules.rulesDict[neighbourhoodVal];
		};
		this.nextState = nextGen.copy;
		^nextGen;
	}
}