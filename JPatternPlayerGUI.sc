/*TODO:
add depth to desc
makesure isPlaying doesn't do stupid stuff
*/

JPatternPlayerGUI{
	var <>paramX, <>paramY,
	/*<>paramXMin, <>paramXMax,*/
	<>paramXTempMax, <>paramXTempMin,
	/*<>paramYMin, <>paramYMax, */
	<>paramYTempMax, <>paramYTempMin,
	<>dur, <>density, <>units,
	<>synth,
	<>paramXName, <>paramYName, <>synthName,
	<>depth,
	<>host,
	<>voices,
	// GUI SPECIFIC
	<>h, <>w, <>m, <>sec,
	<>master,
	<>playControlsView,
	  <>startButton, <> stopButton, <>initButton,
	  <>unitsText, <>densityText, <>depthText,
	<>trackControlsView,
	  <>labels, <>regenerateButton, <>revertButton, <>stitchButton, <>copyButton, <>saveButton, <>memoryButtonArray,<>volSlider,
	<>densityControlView,
	  <>densityLabel, <>densitySlider,
	<>paramXControlView,
	  <>paramXText, <>paramXSlider,
	<>paramYControlView,
	  <>paramYText, <>paramYSlider,
	<>unitsControlView,
	  <>unitButtons,
	<>densityControlView,
	  <>densityText, <>densityButtons, <>densityArray
	;

	classvar <>alreadyExists;
	*new { |mySynth, dur, density, units, depth|
		^super.new.init(mySynth, dur, density, units, depth) }

	*initClass {
		//guarentee singleton
		if (JPatternPlayerGUI.alreadyExists == true, {"ABORT".postln; ^nil},
			{ JPatternPlayerGUI.alreadyExists = true; });
	}
	init { | mySynth, dur, density, units, depth|
		// instance constructor

		this.density = density;
		if (depth == nil, {this.depth = 5}, {this.depth = depth});
		this.units = units;
		this.dur = dur;
		this.synth = mySynth; // a SynthForXYPlayer, see GUIforXYPlayer
		this.synthName = mySynth.synthName;
		this.host = NetAddr("localhost", 4859);

		this.initializeSynth();
		this.createVoices();
		this.patternInit();

		// GUI
		this.h = 40; this.m = 6; this.w = 80;
		this.sec = (this.h + (2 * this.m));
		this.trackControlsView = [];
		this.buildGUI();



	}

	createVoices {
		this.voices = [JPatternPlayerVoice(this, \main, 1), JPatternPlayerVoice(this, \alt, -1)];
		this.voices[0].postln;
	}

	createDesc {

		^(this.units.asString ++ "/" ++ this.density.asStringPrec(2));
	}

	initializeSynth {

		this.paramXTempMax = this.synth.paramXMax;
		this.paramXTempMin = this.synth.paramXMin;
		this.paramYTempMax = this.synth.paramYMax;
		this.paramYTempMin = this.synth.paramYMin;


	}

	buildGUI {
		var unitsArray;

		this.master = Window(this.synthName.asString, Rect(1300,0, 1020, 750)).front.alwaysOnTop_(true);
		this.master.view.decorator_(FlowLayout(this.master.bounds, this.m@this.m, this.m@this.m));
		this.master.onClose_({this.end()});

		// playControlsView (top)
		this.playControlsView = CompositeView(this.master, 1000@(this.sec));
		this.playControlsView.decorator_(FlowLayout(this.playControlsView.bounds, this.m@this.m, this.m@this.m));
		this.playControlsView.background_(Color.red);
		this.startButton = Button(this.playControlsView, this.w@this.h)
			.states_([["start", Color.green]])
			.action_({
				this.startPmonos();
				"GUI:Started".postln;
				this.startButton.states_([["playing", Color.blue]]);
			});
		this.stopButton = Button(this.playControlsView, this.w@this.h)
			.states_([["stop", Color.red]])
			.action_({
			this.voices.do {|voice| voice.pmono.stop()};
				"GUI:both players stopped".postln;
				this.startButton.states_([["play: stopped", Color.green]]);
			});
		this.initButton = Button(this.playControlsView, this.w@this.h)
			.states_([["init", Color.red]])
			.action_({
				this.patternInit();
				"GUI: pattern reinitialized".postln;
			// this.startButton.states_([["play: stopped", Color.green]]);
			});
		this.unitsText = StaticText(this.playControlsView, this.w@this.h).stringColor_(Color.black);
		this.densityText = StaticText(this.playControlsView, this.w@this.h).stringColor_(Color.black);
		this.depthText = StaticText(this.playControlsView, this.w@this.h).stringColor_(Color.black);
		this.updateUnits();
		this.updateDensity();
		this.updateDepth();



		// trackControlsView (bottomLeft) contains 2 tracks
		this.labels = [];
		this.regenerateButton = [];
		this.revertButton = [];
		this.stitchButton = [];
		this.copyButton = [];
		this.saveButton = [];
		this.memoryButtonArray = [];
		this.volSlider = [];

		this.voices.do { |voice, i|
			this.trackControlsView = this.trackControlsView.add(CompositeView(this.master, 1000@130));
			this.trackControlsView[i].decorator_(FlowLayout(this.trackControlsView[i].bounds, (this.m * 4)@(this.m * 4), (this.m * 4)@(this.m * 4)));
			this.trackControlsView[i].background_(Color.black);

			this.labels = this.labels.add(StaticText(this.trackControlsView[i], this.w@this.h));
			this.labels[i].string = (this.voices[i].name ++ "\n" ++ this.voices[i].currentDesc);
			this.labels[i].stringColor = Color.white;

			this.regenerateButton = this.regenerateButton.add(Button(this.trackControlsView[i], this.w@this.h)
			   .states_([["regenerate", Color.blue]])
				.action_({this.voices[i].update(this.voices[i].varCycle(),this.createDesc());
			this.labels[i].string = (this.voices[i].name ++ "\n" ++ this.voices[i].currentDesc);}));

			this.revertButton = this.revertButton.add(Button(this.trackControlsView[i], this.w@this.h)
			   .states_([["revert", Color.blue]])
				.action_({this.voices[i].revert();
			this.labels[i].string = (this.voices[i].name ++ "\n" ++ this.voices[i].currentDesc);}));

			this.stitchButton = this.stitchButton.add(Button(this.trackControlsView[i], this.w@this.h)
			   .states_([["stitch", Color.blue]])
				.action_({this.voices[i].stitch();
			this.labels[i].string = (this.voices[i].name ++ "\n" ++ this.voices[i].currentDesc);}));

			this.copyButton = this.copyButton.add(Button(this.trackControlsView[i], this.w@this.h)
			   .states_([["copy", Color.blue]])
				.action_({this.voices[(i+1)%2].update(this.voices[i].current, this.voices[i].currentDesc ++ "c");
			this.labels[(i+1)%2].string = (this.voices[(i+1)%2].name ++ "\n" ++ this.voices[(i+1)%2].currentDesc);}));

			this.saveButton = this.saveButton.add(Button(this.trackControlsView[i], this.w@this.h)
			   .states_([["save", Color.blue]])
				.action_({
					var count = (this.voices[i].memoryCount % this.voices[i].memorySize);
					this.voices[i].save();
					this.memoryButtonArray[i][count]
					.states_([[ this.voices[i].currentDesc]])
					.action_({this.voices[i].update(this.voices[i].memory[count], this.voices[i].memoryDesc[count]);
					this.labels[i].string = (this.voices[i].name ++ "\n" ++ this.voices[i].currentDesc);});

			}));
			this.memoryButtonArray = this.memoryButtonArray.add([]);
			4.do {|ind|
				this.memoryButtonArray[i] = this.memoryButtonArray[i].add(Button(this.trackControlsView[i], (this.w-20)@this.h)
					.states_([["x", Color.black]]);
				)
			};
			this.volSlider = this.volSlider.add(Slider(this.trackControlsView[i],900@(this.h/2))
				.value_(0.5)
				.action_({|sl| this.voices[i].volMod = sl.value*2;}));

		};


		// parmControls
		this.paramXControlView = CompositeView(this.master, (1000)@(80));
		this.paramXControlView.decorator_(FlowLayout(this.paramXControlView.bounds, this.m@this.m, this.m@this.m));
		this.paramXControlView.background_(Color.black);

		this.paramXText = StaticText(this.paramXControlView, this.w@this.h)
		  .string_(this.synth.paramXName)
		  .stringColor_(Color.white);

		this.paramXSlider = RangeSlider(this.paramXControlView, 900@this.h)
		.lo_(0)
		.hi_(0.2)
		.action_({|sl| this.paramXTempMin = this.map(sl.lo, this.synth.paramXMin, this.synth.paramXMax);
			this.paramXTempMax = this.map(sl.hi, this.synth.paramXMin, this.synth.paramXMax) });


		this.paramYControlView = CompositeView(this.master, (1000)@(80));
		this.paramYControlView.decorator_(FlowLayout(this.paramYControlView.bounds, this.m@this.m, this.m@this.m));
		this.paramYControlView.background_(Color.black);

		this.paramYText = StaticText(this.paramYControlView, this.w@this.h)
		  .string_(this.synth.paramYName)
		  .stringColor_(Color.white);

		this.paramYSlider = RangeSlider(this.paramYControlView, 900@this.h)
		.lo_(0)
		.hi_(0.2)
		.action_({|sl| this.paramYTempMin = this.map(sl.lo, this.synth.paramYMin, this.synth.paramYMax);
			this.paramYTempMax = this.map(sl.hi, this.synth.paramYMin, this.synth.paramYMax) });

		this.densityControlView  = CompositeView(this.master, 1000@80);
		this.densityControlView.decorator_(FlowLayout(this.densityControlView.bounds, this.m@this.m, this.m@this.m));
		this.densityControlView.background_(Color.black);

		this.densityLabel = StaticText(this.densityControlView, this.w@this.h)
		.string_("density")
		.stringColor_(Color.white);

		this.densitySlider = Slider(this.densityControlView, 900@this.h)
		.action_({|sl| this.updateDensity(sl.value); this.density.postln;});


		this.unitsControlView  = CompositeView(this.master, 1000@80);
		this.unitsControlView.decorator_(FlowLayout(this.unitsControlView.bounds, this.m@this.m, this.m@this.m));
		this.unitsControlView.background_(Color.black);

		unitsArray =  [2,3,4,5,6,7,8,9,10,12,14,16,24,32];
		this.unitButtons = [];
		StaticText(this.unitsControlView, this.w@this.h).string_("units").stringColor_(Color.white);
		unitsArray.do { |val, i|
			this.unitButtons = this.unitButtons.add(Button(this.unitsControlView, (this.w-24)@this.h)
				.states_([[unitsArray[i], Color.blue]])
				.action_({this.updateUnits(unitsArray[i]);}));
		}
	}

	updateDepth{|newVal|
		if (newVal != nil, {this.depth = newVal});
		this.depthText.string_("depth: " ++ this.depth);
	}
	updateDensity{ |newVal|
		if (newVal != nil, {this.density = newVal});
		this.densityText.string_("density: " ++ this.density.asStringPrec(2));
	}
	updateUnits{ |newVal|
		if (newVal != nil, {this.units = newVal});
		this.unitsText.string_("units: " ++ this.units);

	}


	patternInit{
		this.voices[0].update(this.voices[0].varCycle(),this.createDesc);
		this.voices[1].update(this.voices[0].current, this.createDesc);
	}
	startPmonos {
		this.voices.do{|voice| voice.doPmono()};
	}

	map{ |inVal, minRange, maxRange|
		var dist = maxRange - minRange;
		^inVal.linexp(0.0, 1.0, minRange, maxRange);
	}

	end{
		this.voices.do { |voice|
			voice.pmono.stop();
		}
	}

}

JPatternPlayerVoice {
	var <>current, <>prev, <>pmono, <>panPos, <>owner, <>name, <>memory, <>memorySize, <>currentDesc, <>prevDesc, <>memoryDesc, <>memoryCount, <>volMod;
	*new { |owner, name, panPos|
		^super.new.init(owner,name,panPos); }
	init{ |owner,name,panPos|
		this.owner = owner;
		this.name = name;
		this.panPos = panPos;
		this.memorySize = 4;
		this.memoryCount = 0;
		this.volMod = 1;
		this.memoryDesc = Array.fill(this.memorySize,{});
		this.memory = Array.fill(this.memorySize,{});

	}

	update{ |newPat, newDesc|
		// bookkeeping: keep move displaced pattern to previous
		this.prev = this.current;
		this.prevDesc = this.currentDesc;
		this.current = newPat;
		this.currentDesc = newDesc;

	}
	revert{
		var temp = this.current, tempDesc = this.currentDesc;
		if (this.prev == nil,
			{ this.prev = this.current;  this.prevDesc = this.currentDesc;},
			{ this.current = this.prev;  this.currentDesc = this.prevDesc;});
		this.prev = temp;
		this.prevDesc = tempDesc;
	}

	stitch{
		var temp = this.prev ++ this.current, tempDesc = this.prevDesc ++ this.currentDesc;

		this.update(temp, tempDesc);
	}

	// maybe need to subclass this:
	doPmono{
		var send;
		send =  {
			[~name, ~p1, ~p2, ~amp].postln;
			// this.owner.host.sendMsg(~name, ~p1, ~p2.asInteger, ~amp.asFloat);
		this.owner.host.sendMsg(~name, ~p1, ~p2, ~amp.asFloat);
	};

		this.pmono = Pmono( this.owner.synthName,
			[this.owner.synth.paramXName,this.owner.synth.paramYName, \ampIn], Pn(Plazy{Pseq(this.current)}),
			[\p1,\p2, \amp], Pn(Plazy{Pseq(this.current)}),
			\amp, Pkey(\ampIn)*Pn(Plazy{this.volMod}),
			\dur, this.owner.dur,
			\name, this.name,
			\finish, send,
			\pan, this.panPos;

		).play();
	}

	*createDeleteTemplate  { |length, density|
		//density and length stored in mainGUI
		var numToDelete = (1-density)*length, selectFrom, res;
		if (length == 1, {res = [[1]]}, {
			numToDelete = numToDelete.round(1).asInteger;
			if (numToDelete >= length, {numToDelete =( numToDelete -1)});
			selectFrom = (1..(length - 1));

			selectFrom = selectFrom.scramble;
			selectFrom = selectFrom[0..(numToDelete-1)];

			res = Array.fill(length, {1});
			selectFrom.do {|val| res[val] = 0 };});
		^res
	}

	generate { |length| var meterList, res = [], deleteList;
		// depth and density in mainGUI
		meterList = MeterTree(length,this.owner.depth).get_surface_vals;
		deleteList = JPatternPlayerVoice.createDeleteTemplate(meterList.size, this.owner.density);
		meterList.do { |val,i|
			var p1 = 0.0, p2 = 0,  amp, basicAmp = 0.6;
			if (deleteList[i] == 0, {amp = 0}, {amp = basicAmp});

			p1 = exprand(this.owner.paramXTempMin, this.owner.paramXTempMax);
			p2 = exprand(this.owner.paramYTempMin, this.owner.paramYTempMax);

			val.do {
				res = res.add([p1, p2, amp])
			};

		};
		["in generate, res size", res.size	].postln;
		res.postln;
		^res
	}

	varCycle {
		// generate extended pattern (returns pattern - no side effects)
		var options = [], patterns = [[0,1,0,2],[0,0,0,1],[0,1,0,1]], result, choice;
		3.do {options = options.add(this.generate(this.owner.units)) };
		choice = patterns.choose;
		// choice.postln;
		choice.do { |i|
			var temp = options[i].copy;
			options[i].postln;
			result = result ++ temp;
		};
		^result;
	}

	save {
		this.memory[this.memoryCount % this.memorySize] = this.current;
		this.memoryDesc[this.memoryCount % this.memorySize] = this.currentDesc;
		this.memoryCount = this.memoryCount + 1;
	}

}