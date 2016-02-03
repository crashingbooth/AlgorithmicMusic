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

	restartInput {
		this.currentNoteIn = MarkovQueue(this.nOrder);
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

MarkovPlayerDrumManager {
	var <>tempoclock, <>midiout, <>drums, <>isPlaying;
	*new { |tempoclock, midiout|
		^super.new.init(tempoclock, midiout) }
	init { |tempoclock, midiout|
		this.tempoclock = tempoclock;
		this.midiout = midiout;
		this.isPlaying = false;
	}

	playDrums {
		var aTask;
		if (this.isPlaying == false, {
			this.isPlaying = true;
			this.drums = DrumPlayer(this.midiout, this.tempoclock);
			this.drums.playMode_(\playSingle);
			this.drums.setCurrentPattern(this.drums.generatePattern(2,4,minDensity: 0.2, maxDensity: 0.5));
			aTask = Task({
				this.drums.play();}).play(this.tempoclock, quant:[4,0]);
	});
	}
}


MarkovPlayer {
	// listening to channel 2(1 in sc), playing on channel 3(2 in sc)
	var <>markovRoot, <>listener, <>midiout, <>meterTreeOutput, <>count, <>voicedLimit, <>myPbind ,<>autoRegenMode, <>tempoclock, <>drumManager, <>length, <>depth, <>mode, <>legato, <>outChannel;
	*new { |nOrder, outChannel, tempoclock|
		^super.new.init(nOrder,outChannel, tempoclock) }
	init { |nOrder, outChannel, tempoclock|
		this.markovRoot = MarkovRoot(nOrder);
		MIDIClient.init;
		this.count = -1;
		this.midiout =  MIDIOut(0);
		this.voicedLimit = 1.0;
		this.autoRegenMode = false;
		this.tempoclock = tempoclock;
		this.length = 16; //default
		this.depth = 6; // default
		this.mode = 'meterTree_stable';
		this.legato = 0.5;
		this.outChannel = outChannel;
		this.listener(MarkovInputListener(this.markovRoot));
	}

	generateMeterTreeOutput {
		var rhythm, result = [], voicedLimit, totalVoiced = 0;
		rhythm = MeterTree(this.length, this.depth).get_surface_vals;

		this.markovRoot.currentNoteOut = MarkovQueue(this.markovRoot.nOrder);
		rhythm.do { |dur, i|
			totalVoiced = totalVoiced + dur;
			if ((totalVoiced/this.length) <= this.voicedLimit,
				{result = result.add([this.markovRoot.playNote(), dur])},
				{result = result.add([\rest, dur])});
		};
		result.postln;
		this.meterTreeOutput = result;
	}



	initializeModel { |listOfNotes|
		this.markovRoot.initializeModel(listOfNotes);
	}

	getMeterTreeOutput { |countModulo|

		["in getMeterTreeOutput", count, this.mode].postln;
		if (this.mode != 'meterTree_stable', {this.count = this.count + 1}, {this.count = -1});
		if ((this.count % countModulo) == 0, { this.generateMeterTreeOutput});
		"here".postln;
		this.meterTreeOutput.postln;
		^this.meterTreeOutput;
	}

	getData {
		var result; // passage of length L
		// make sure that model isn't empty


		if (this.markovRoot.nodeDict.size == 0, {this.mode = 'blank'});

		case
		{this.mode == 'meterTree1'} {result = this.getMeterTreeOutput(1)}
		{this.mode == 'meterTree2'} {result = this.getMeterTreeOutput(2)}
		{this.mode == 'meterTree4'} {result = this.getMeterTreeOutput(4)}
		{this.mode == 'meterTree_stable'} {result = this.getMeterTreeOutput(100000)}
		{this.mode == 'simple'} {result = this.getSimpleOutput()}
		{this.mode == 'blank'} {result = this.getBlankOutput()};
		result.postln;
		^result
	}
	getOutput {
		// wrapper function for getData, override in subclass
		^this.getData();

	}

	playMeterTreeOutput {


		if (this.meterTreeOutput == nil, {this.generateMeterTreeOutput});
		this.myPbind = Pbind (
			\type, \midi,
			\midiout, this.midiout,
			[\midinote, \dur],  Pn(Plazy {Pseq(this.getOutput)}),
			// [\midinote, \dur],  Pn(Plazy {Pseq(this.meterTreeOutput,1)}),
			\chan, this.outChannel,
			\stretch, 1,
			\legato, Pn(Plazy {this.legato})
		).play(this.tempoclock,quant:[4,0]);

	}

	getSimpleOutput {
		var result = [];
		this.length.do {
			result = result.add([this.markovRoot.playNote(), 1]);
		};
		result.postln;
		^result;
	}
	getBlankOutput {
		var result;
		"playing blank".postln;
		result = [[\rest, this.length]];
		^result;
	}


	reset {
		this.markovRoot.reset();
	}
	restartInput {
		this.markovRoot.restartInput();
	}
	end {
		this.myPbind.stop();
		this.midiout.allNotesOff(this.outChannel);
	}

}

MarkovPlayerWithDrums : MarkovPlayer {
	var <>drumManager;
	*new { |nOrder, midiout, tempoclock|
		^super.new(nOrder, midiout, tempoclock).sub_init() }

	sub_init {
		this.drumManager = MarkovPlayerDrumManager(this.tempoclock, this.midiout);
	}
	end {
		// override superclass for drum handling
		this.myPbind.stop();
		this.drumManager.drums.pb.stop();
		this.drumManager.drums.beatsched.clear;
		this.midiout.allNotesOff(this.outChannel);
		this.drumManager.isPlaying = false;
	}
	getOutput {
		// wrapper function for getData, override in subclass
		this.drumManager.playDrums();
		^this.getData

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

MarkovPlayerGUI {
	var <>markovPlayer, <>auxMarkovPlayer,
	<>master, <>w,<>h,<>m,<>sec,<>wid,
	<>topLineView,
	  <>inputRestartButton, <>resetButton, <>mostRecentText,
	<>playViewSec,

	<>drumView, <>drumModeView,
	<>drum_regularButton, <>drum_playSingleButton,<>drum_last2Button, <>drum_revertButton,
<>drum_evolveButton, <>fill1Buttons, <>fill2Buttons, <>localMinDensity, <>localMaxDensity, <>drum_fillDensitySlider, <>drum_recButtons ,<>drum_playButtons,<>drum_bank,<>drumCurrentString, <>drumLastString,<>drumCurrentPatternLabel,<>drumLastPatternLabel,<>fill3Buttons, <>tempoclock
	;

	*new {|nOrder = 2,tempo |
		^super.new.init(nOrder, tempo) }
	init {|nOrder, tempo|
		this.tempoclock = TempoClock(tempo);
		this.markovPlayer = MarkovPlayerWithDrums(nOrder,2, this.tempoclock);
		this.auxMarkovPlayer = MarkovPlayer(nOrder,3, this.tempoclock);
		this.w = 80;
		this.h = 40;
		this.m = 10;
		this.wid = 850;
		this.sec = (this.h + this.m + this.m);
		this.buildGUI();
	}

	*globalKeyDownAction {|view, char|
			if (char == ' ', { this.markovPlayer.restartInput(); "hi".postln;})}
	buildGUI {
		var playViewSec;
		this.master = Window("MarkovPlayer",  Rect(1300,0, this.wid, 1000)).front.alwaysOnTop_(true);
		this.master.view.decorator_(FlowLayout(this.master.bounds, this.m@this.m, this.m@this.m));
		this.master.onClose_({this.end()});


		this.topLineView = CompositeView(this.master, (this.wid -20)@(this.sec));
		this.topLineView.decorator_(FlowLayout(this.topLineView.bounds, this.m@this.m, this.m@this.m));
		this.topLineView.background_(Color.black);


		this.inputRestartButton = Button(this.topLineView, this.w@this.h)
			.states_([["restart", Color.black]])
			.action_({ this.markovPlayer.restartInput();});

		this.resetButton = Button(this.topLineView, this.w@this.h)
			.states_([["reset", Color.black]])
			.action_({
			  this.markovPlayer.reset();
			});


		this.playViewSec = MarkovGUIPlayViewSection(this.master, this.markovPlayer, this.wid, this.sec, this.h, this.m, this.w);
		this.playViewSec = MarkovGUIPlayViewSection(this.master, this.auxMarkovPlayer, this.wid, this.sec, this.h, this.m, this.w);


		this.drumView = CompositeView(this.master, 600@550);
		this.makeDrumViewSection()
	}
	//DRUMVIEW SECTION  (600@550)
	makeDrumViewSection {
		var w = 100, changeView, fill1View, fill2View, fill3View, recView, playView, bankSize = 4;
		var fill1List, fill2List, fill3List;
		this.drumView.decorator_(FlowLayout(this.drumView.bounds, this.m@this.m,this.m@this.m));
		this.drumCurrentPatternLabel = StaticText(this.drumView, 250@this.h);
		this.drumCurrentPatternLabel.stringColor_(Color.black);
		// this.drumCurrentPatternLabel.string_(this.markovPlayer.drums.currentPattern.name);
		this.drumLastPatternLabel = StaticText(this.drumView, 250@this.h);

		this.drumView.decorator.nextLine;
		this.drumModeView = HLayoutView(this.drumView, Rect(5,5,310,this.h));
		this.drumModeView.background(Color.red);
		this.drum_regularButton = Button(this.drumModeView,w*1 )
		.states_([["regular", Color(), Color.gray(0.9)]])
		.action_({this.drum_behaviour(\playRegularPolymetric)});
		this.drum_playSingleButton = Button(this.drumModeView,w*1)
		.states_([["single repeat", Color(), Color.gray(0.9)]])
		.action_({this.drum_behaviour(\playSingle)});
		this.drum_last2Button = Button(this.drumModeView,w )
		.states_([["2 bar repeat", Color(), Color.gray(0.9)]])
		.action_({this.drum_behaviour(\playLastTwo)});
		changeView = HLayoutView(this.drumView, Rect(0,0,250,this.h));
		this.drum_revertButton = Button(changeView,w )
		.states_([["revert", Color(), Color.gray(0.9)]])
		.action_({this.drum_restoreLastPattern});

		this.drum_evolveButton = Button(changeView,w )
		.states_([["evolve", Color(), Color.gray(0.9)]])
		.action_({this.drum_evolve});
		fill1View = VLayoutView(this.drumView, Rect(5,5,180, this.h*4));
		this.fill1Buttons = [];
		fill1List = [[1,16], [2,6], [3,4],[4,3]];
		fill2List = [[1,8], [2,4], [2,8], [4,4]];
		4.do{ |i| this.fill1Buttons = this.fill1Buttons.add(Button(fill1View, this.h)
			.states_([[fill1List[i][0].asString ++ " x " ++ fill1List[i][1].asString ++ " basic"]])
			.action_({this.drum_basicFill(fill1List[i][0], fill1List[i][1])})
		)};
		fill2View = VLayoutView(this.drumView, Rect(5,5,180,this.h*4));
		this.fill2Buttons = [];
		4.do{ |i| this.fill2Buttons = this.fill2Buttons.add(Button(fill2View, this.h)
			.states_([[fill2List[i][0].asString ++ " x " ++ fill2List[i][1].asString ++ " basic"]])
			.action_({this.drum_basicFill(fill2List[i][0], fill2List[i][1])})
		) };
		fill3List = [[1,3],[2,2],[2,4],[1,1]];
		fill3View = VLayoutView(this.drumView, Rect(5,5,180, this.h*4));
		this.fill3Buttons = [];
		4.do{ |i| this.fill3Buttons = this.fill3Buttons.add(Button(fill3View, this.h)
			.states_([[fill3List[i][0].asString ++ "bar: " ++ fill3List[i][1] ++"s polymetric"]])
			.action_({this.drum_polymetricFill(fill3List[i][0],fill3List[i][1])});
		) };
		this.drumView.decorator.nextLine;
		this.localMinDensity = 0;
		this.localMaxDensity = 0.5;
		this.drum_fillDensitySlider = RangeSlider(this.drumView, 550@this.h)
		.lo_(this.localMinDensity)
		.hi_(this.localMaxDensity)
		.action_({|sl| this.localMinDensity = sl.lo; this.localMaxDensity = sl.hi; });

		this.drumView.decorator.nextLine;
		playView = VLayoutView(this.drumView, Rect(5,5,180,this.h*bankSize));
		recView = VLayoutView(this.drumView, Rect(5,5,40,this.h*bankSize));
		this.drum_recButtons = [];
		this.drum_playButtons = [];
		this.drum_bank = Array.fill(bankSize, {nil});
		bankSize.do{ |i|
			this.drum_recButtons = this.drum_recButtons.add(Button(recView, this.h)
				.states_([["", Color(), Color.red]])
				.action_({this.drum_bank[i] = this.drum_getCurrentPattern;
					this.drum_playButtons[i].states_([[this.drum_bank[i].name]])})
			);
			this.drum_playButtons = this.drum_playButtons.add(Button(playView, this.h)
				.action_({
					if (this.drum_bank[i] != nil, {this.drum_setCurrentPattern(this.drum_bank[i])});
					})


			);

		 };

		}
	drumModeButtonsReset {
		//<>drum_regularButton, <>drum_playSingleButton, <>drum_last2Button,
		this.drum_regularButton.setBackgroundColor(0, Color.gray(0.9));
		this.drum_playSingleButton.setBackgroundColor(0, Color.gray(0.9));
		this.drum_last2Button.setBackgroundColor(0, Color.gray(0.9));
	}

	drum_behaviour { |behaviour|
		// \playSingle, \playLastTwo, \playRegularPolymetric
		this.markovPlayer.drumManager.drums.playMode = behaviour;
		this.updateDrumLabels();
	}

	drum_restoreLastPattern {
		this.markovPlayer.drumManager.drums.restoreLastPattern()
	}



	drum_polymetricFill { |numBars, unit|
		this.markovPlayer.drumManager.drums.polymetricFill(numBars,unit);
		//e.g. this.drums.polymetricFill(1,3) -> one bar of triplets
		this.updateDrumLabels();
	}

	drum_evolve {
		var minDens, maxDens;
		minDens = this.localMinDensity;
		maxDens = this.localMaxDensity;
		this.markovPlayer.drumManager.drums.evolveLastFill(minDens, maxDens);
		this.updateDrumLabels();
	}

	drum_basicFill { |unitLength, numReps|
		var minDens, maxDens;
		minDens = this.localMinDensity;
		maxDens = this.localMaxDensity;
		this.drum_setCurrentPattern(this.markovPlayer.drumManager.drums.generatePattern(unitLength,numReps,minDensity: minDens, maxDensity: maxDens));

	}

	drum_getCurrentPattern {
		^this.markovPlayer.drumManager.drums.currentPattern;
	}

	drum_setCurrentPattern { |pattern|
		this.markovPlayer.drumManager.drums.setCurrentPattern(pattern);
		this.updateDrumLabels();
	}
	updateDrumLabels {
		this.drumCurrentPatternLabel.string_(this.markovPlayer.drumManager.drums.currentPattern.name);
		this.drumLastPatternLabel.string_(this.markovPlayer.drumManager.drums.lastPattern.name)
	}
	end {
		this.markovPlayer.end();
		this.auxMarkovPlayer.end();
	}


}

MarkovGUIPlayViewSection {
	var <>playView, <>master, <>wid, <>sec, <>h, <>m, <>w, <>markovPlayer,
	<>modesList, <>modesView, <>playButton, <>stopButton, <>regenButton, <>silenceSlider, <>legatoSlider;

	*new { |master, markovPlayer, wid, sec, h, m, w|
		^super.new.init(master, markovPlayer, wid, sec, h, m, w) }

	init { |master, markovPlayer, wid, sec, h, m, w|
		this.master = master;
		this.markovPlayer = markovPlayer;
		this.wid = wid; this.sec = sec;
		this.h = h; this.m = m; this.w = w;
		this.buildPlayView();
	}

	buildPlayView {

		this.playView = CompositeView(this.master, ((this.wid -30)/2)@((this.sec) + (2*this.h) + this.m));
		this.playView.decorator_(FlowLayout(this.playView.bounds, this.m@this.m, this.m@this.m));
		this.playView.background_(Color.black);

		this.playButton = Button(this.playView, this.w@this.h)
			.states_([["play", Color.black]])
			.action_({
			  this.markovPlayer.playMeterTreeOutput();
			});

		this.stopButton = Button(this.playView, this.w@this.h)
			.states_([["stop", Color.black]])
			.action_({
			this.markovPlayer.end();
			});
		this.regenButton = Button(this.playView, this.w@this.h)
			.states_([["regen", Color.black]])
			.action_({ this.markovPlayer.generateMeterTreeOutput();
		});



		this.modesList = [ 'meterTree_stable','meterTree1', 'meterTree2', 'meterTree4', 'simple', 'blank'];
		this.modesView = ListView(this.playView ,((this.w*2) - 50)@(2*this.h))
        .items_(this.modesList)
        .background_(Color.clear)
        .hiliteColor_(Color.green(alpha:0.6))
        .action_({|textList|this.markovPlayer.mode = this.modesList[textList.value]    });

		this.silenceSlider = Slider(this.playView, ((this.wid/2) -(this.m*4))@(this.h/2))
			.value_(0.9)
			.action_({|sl| this.markovPlayer.voicedLimit = sl.value});
		this.legatoSlider = Slider(this.playView, ((this.wid/2) -(this.m*4))@(this.h/2))
			.value_(0.3)
			.action_({|sl| this.markovPlayer.legato = sl.value});



	}


}