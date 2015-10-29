MarkovEndNode {
	var <>history, <>self;

	*new { |self|
		^super.new.init(self) }

	init { |self|
		this.self = self;
		this.history = []
	}

	addNote { |newNote|
		this.history = this.history.add(newNote);
	}

	getNote {
		if (this.history.size == 0,
			{^nil},
			{^this.history.choose()});
	}
}

MarkovRoot {
	// make this into superclass, with this version (1st order) as subclass
	var <>nodeDict, <>root, <>currentNoteIn, <>currentNoteOut, <>nOrder;
	*new { |nOrder|
		^super.new.init(nOrder) }
	init { |nOrder|
		// instance constructor
		this.nOrder = nOrder;
		this.root = root;
		this.nodeDict = Dictionary.new();
		this.setInOutToRoot();
		this.nodeDict.put(MarkovQueue(this.nOrder).convertToSymbol, MarkovEndNode(root));
	}

	setInOutToRoot{
		this.currentNoteIn = MarkovQueue(this.nOrder);
		this.currentNoteOut = MarkovQueue(this.nOrder);
	}


	listenNote { |newNote|
		if (this.nodeDict.keys.includes(this.currentNoteIn.convertToSymbol) == false ,
			{
				this.nodeDict.put(this.currentNoteIn.convertToSymbol, MarkovEndNode(currentNoteIn.convertToSymbol));
			});
		this.nodeDict[this.currentNoteIn.convertToSymbol].addNote(newNote);
		["added", this.currentNoteIn.queue,"->", newNote].postln;
		this.currentNoteIn.pushQueue(newNote);

	}


	initializeModel { |listOfNotes|
		listOfNotes.do { |note|
			this.listenNote(note);
		}
	}

	reset {
		this.nodeDict = Dictionary.new();
		this.setInOutToRoot();
	}

	playNote {
		var nextNote;
		if (this.nodeDict.keys.includes(this.currentNoteOut.convertToSymbol) == false, {
			["no node for" , this.currentNoteOut.convertToSymbol].postln;
			this.currentNoteOut = MarkovQueue(this.nOrder);
			^this.playNote()},
			{
				nextNote = this.nodeDict[this.currentNoteOut.convertToSymbol].getNote();
				this.currentNoteOut.pushQueue(nextNote);
				["playing", nextNote].postln;
				^nextNote;

		});

	}
}

MarkovQueue {
	var <> nOrder, <>queue;
	*new { |nOrder|
		^super.new.init(nOrder) }
	init { |nOrder|
		this.nOrder = nOrder;
		this.queue = [];
		this.makeBlankQueue();
	}

	makeBlankQueue {
		this.nOrder.do {this.queue = this.queue.add(0); };
	}

	pushQueue { |newNote|
		if (this.nOrder > 1, {
			this.queue.addFirst(newNote);
			this.queue.pop();},
			{this.queue = [newNote]});
	}

	convertToSymbol {
		var result = "";
		this.queue.do{ |e|
			result = result ++ "a";
			result = result ++ e.asString;
		}
		^result.asSymbol;
	}

	convertToList {|symbolForm|
		var result = [], temp;


		symbolForm.asString.do { |char, i|
			if (char.asString == "a",
				{
					if (temp.size > 0,
						{
							result = result.add(temp.asInteger);
							temp = "";
						}
					);
				},
				{temp = temp ++ char});
		};
		result = result.add(temp.asInteger);
		^result;

	}

}

MarkovPlayer {
	// listening to channel 1(0), playing on channel 2(1)
	var <>markovRoot, <>listener, <>midiout, <>meterTreeOutput, <>count;
	*new { |nOrder|
		^super.new.init(nOrder) }
	init { |nOrder|
		this.markovRoot = MarkovRoot(nOrder);
		MIDIClient.init;
		this.count = 0;
		this.midiout =  MIDIOut(0);
		this.listener(MarkovInputListener(this.markovRoot));
	}

	generateMeterTreeOutput { |length = 32, depth = 6|
		var rhythm, result = [];
		rhythm = MeterTree(length, depth).get_surface_vals;
		this.markovRoot.currentNoteOut = MarkovQueue(this.markovRoot.nOrder);
		rhythm.do { |dur, i|

				result = result.add([this.markovRoot.playNote(), dur])
		};
		result.postln;
		this.meterTreeOutput = result;
	}

	initializeModel { |listOfNotes|
		this.markovRoot.initializeModel(listOfNotes);
	}

	getMeterTreeOutput {
		this.count = this.count + 1;
		if ((this.count % 2) == 0, { this.generateMeterTreeOutput});
		"here".postln;
		this.meterTreeOutput.postln;
		^this.meterTreeOutput;
	}

	playMeterTreeOutput {
		if (this.meterTreeOutput == nil, {this.generateMeterTreeOutput});

		Pbind (
			\type, \midi,
			\midiout, this.midiout,
			[\midinote, \dur],  Pn(Plazy {Pseq(this.getMeterTreeOutput)}),
			// [\midinote, \dur],  Pn(Plazy {Pseq(this.meterTreeOutput,1)}),
			\chan, 1,
			\stretch, 0.2,
		).play;

	}


	getSimpleOutput { |duration= 1|
		Pbind (
			\type, \midi,
			\midiout, this.midiout,
			\midinote,  Pn(Plazy {this.markovRoot.playNote()}),
			\dur, duration,
			\chan, 1,
			\stretch, 1,
		).play;
	}

	reset {
		this.markovRoot.reset();
	}
}

MarkovInputListener {
	var <>markovRoot;
	*new {|markovRoot|
		^super.new.init(markovRoot) }
	init {|markovRoot|
		this.markovRoot = markovRoot;
		MIDIIn.connectAll;
		MIDIFunc.noteOn({ |veloc, num, chan, src|
			if ((chan == 0) && (veloc > 20), {this.markovRoot.listenNote(num);});
});
	}

}