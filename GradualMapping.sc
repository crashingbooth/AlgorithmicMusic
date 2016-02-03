/*Class for taking two rhythms in the form [[dur,note/rest]] and morphing one to the other
_playGradations will slowly morph one to the other over specified duration
_livePlay allows relative position to be controlled in real time

GradualMappingGUI uses the touchOSC gui 'gradualMappingGUI'*/

GradualMapping {
	var <> startList, <> destList, <>length, <>posInput, <>gradList, <>automaticMode, <>owner, <>slider, <>pbs, <>midiout;
	*new { |startList, destList|
		^super.new.init(startList, destList) }

	init { |startList, destList|
		// instance constructor
		if (destList == nil, {destList = startList});
		this.validateLists(startList, destList).postln;
		this.automaticMode = false;
		this.startList = startList;
		this.destList = destList;
		this.posInput = 0;


	}

	createSliders { |owner|
		this.owner = owner;
		this.slider = Slider(this.owner.master, 900@40)
		.value_(0.0)
		.action_({|sl| this.posInput = sl.value})
		;
	}

	setSlider { |newValue|
		this.slider.value = newValue;
		this.posInput = newValue;
	}

	calculatePositions{|aList|
		// converts a list of durations to a list of bar positions
		var realPos = [], pos = 0;
		aList.do {|val, i|
			realPos = realPos.add([pos, val[1]]);
			pos = pos + val[0];
		};
		^realPos
	}

	calculateDurations {|aList|
		// converts a list of positions to durations
		var durs = [], prevPos = 0, prevVal;
		aList.do {|val,i|
			if (i != (aList.size-1), {
				durs = durs.add([aList[i+1][0] - val[0], val[1]]);
				}, { durs = durs.add([this.length - val[0], val[1]]);
			});
		};
		^durs
	}


	getGradations{|stepsBtn, direction = 'forward'|
		// start with initial, + stepsBtn + final
		// TODO: make sure rests are okay!!!

		var nodeList = [], destInd = 0, startPoss, destPoss, tempPoss, result = [], begin, end;

		case
		{direction == 'forward'} { begin = startList; end = destList }
		{direction == 'backward'} { begin =  destList; end = startList }
		{direction == 'startOnly'} { begin =  startList; end = startList }
		{direction == 'endOnly'} { begin =  destList; end = destList } ;


		startPoss = this.calculatePositions(begin);
		destPoss = this.calculatePositions(end);

		startPoss.do{ |startPossEntry, i|
			// find first node
			var distance;
			if (startPossEntry.[1] != \r, {
				// ignore rests
				while ({destPoss[destInd][1] == \r}, {destInd = destInd +1});
				distance = (startPossEntry[0] - destPoss[destInd][0]);
				nodeList = nodeList.add((-1*distance/(stepsBtn + 1))) ;
				destInd = destInd +1;
			});
		};
		nodeList.dopostln;
		result = result.add(this.calculateDurations(startPoss));
		tempPoss = startPoss.deepCopy;
		stepsBtn.do {
			var next;
			tempPoss.do {|entry, i|
				if (entry[1] != \r, {
					tempPoss[i][0] = tempPoss[i][0] + nodeList[i];
				})
			};
			next = this.calculateDurations(tempPoss);
			result = result.add(next);
		};
		result = result.add(end);


		^result
	}


	validateLists{|aList, bList|
		var aNodes = 0, aLength = 0, bNodes = 0, bLength = 0, aRest = false, bRest = false;

		aList.do{ |node, i|
			if (node[0] != \r, {aNodes = aNodes + 1}, {aRest = true});
			aLength = aLength + node[0];
		};
		bList.do{ |node, i|
			if (node[0] != \r, {bNodes = bNodes + 1}, {bRest = true});
			bLength = bLength + node[0];
		};

		if (aRest && (bRest == false), {bRest.insert(0, [0,\r]);});
		if (bRest && (aRest == false), {aRest.insert(0, [0,\r]);});

		if ((aNodes != bNodes) || (aLength.round(0.001) != bLength.round(0.001)), {
			["not validated; numNodes a and b , length:", aNodes, bNodes, aLength.round(0.001) - bLength.round(0.001)].postln;
			^false;
		}, {this.length = aLength; ^true});


	}

	oscillateGradation {|stepsList, midiout, note_, chan_, stretch_|
		// stepsList will go from start to dest, dest to start in in numSteps
		var flatList;
		stepsList.do { |steps, i|
			var isForward, tempGradList;
			if (i % 2 == 0, { isForward = 'forward';}, { isForward = 'backward';});
			tempGradList = this.getGradations(steps, isForward);
			tempGradList.do {|entries|
				this.gradList = this.gradList.add(entries);
			}
		};
		flatList = this.gradList.flatten;
		this.scheduledGradation(flatList, midiout, note_, chan_, stretch_);
	}
	plannedGradation {|stepsList, midiout, note_, chan_, stretch_|
		// stepsList
		var flatList;
		stepsList.do { |steps, i|
			var tempGradList;

			tempGradList = this.getGradations(steps[0], steps[1]);
			tempGradList.do {|entries|
				this.gradList = this.gradList.add(entries);
			}
		};
		flatList = this.gradList.flatten;
		this.scheduledGradation(flatList, midiout, note_, chan_, stretch_);

	}

	scheduledGradation {|flatList, midiout, note_, chan_, stretch_|
		this.midiout = midiout;
		this.automaticMode = true;
		this.pbs = 	Pbind (
			\type, \midi,
			\midiout, this.midiout,
			[\dur, \degree], Pseq(flatList),
			\root, note_,
			\chan, chan_,
			\stretch, stretch_,
			\scale, Scale.chromatic
				).play;
	}
	playGradation {|steps, midiout, note_, chan_, stretch_|
		var flatList;
		this.gradList = this.getGradations(steps);
		flatList = this.gradList.flatten;
		this.scheduledGradation(flatList, midiout, note_, chan_, stretch_);


	}

	getPos {
		// overide this in controller versions
			// this.posInput  = this.slider.value;
		^this.posInput;
		// ^this.mouse.getSynchronous;
	}
	getCurrentFromPos {
		var nodeList = [], destInd = 0, startPoss, destPoss, tempPoss, result = [], screenWidth = 1366, mouse, b, percent;
		percent = this.getPos();

		startPoss = this.calculatePositions(startList);
		destPoss = this.calculatePositions(destList);
		startPoss.do{ |startPossEntry, i|
			// find first node
			var distance;
			if (startPossEntry.[1] != \r, {
				// ignore rests
				while ({destPoss[destInd][1] == \r}, {destInd = destInd +1});
				distance = (startPossEntry[0] - destPoss[destInd][0]);
				nodeList = nodeList.add(-1*distance) ;
				destInd = destInd +1;
			});
		};

		["took position:",  percent].postln;
		// nodeList.postln;
		startPoss.do {|val, i|
			result = result.add([val[0] + (nodeList[i]*percent),val[1]]);
		};
		result = this.calculateDurations(result);
	/*	this.startList.postln;
		result.postln;
		this.destList.postln;*/
		^result;



	}
	*wrapper { |simpleList|
		// static method to convert simple list of durations to properly formated list
		var result = [];

		simpleList.do {|num|
			result = result.add([num, 0]);
		}
		^result;
	}

	*convertFromBinary { |binaryList|
		// make a rhythm list from binary rhythm like [1,0,0,1,0,0,1,0] (leverage PolyDrumBinary class)
		var result = [], i= 0, length = 0;
		while ({binaryList[i] == 0}, {length = length + 1; i = i+1;});
		if (length > 0, { result = result.add([length, \r]); length = 0});

		for (i, binaryList.size-1, {
			if ((binaryList[i] == 1) && (length > 0), {
				result = result.add([length, 0]); length = 1; i = i +1;
			}, {
				length = length + 1; i = i + 1;
			});
		});
		result = result.add([length, 0]);
		^result;
	}

	*euclid { |length, pulses|
		var polyDrum = PolyDrumBinary(length);
		polyDrum.euclid(pulses);
		^GradualMapping.convertFromBinary(polyDrum.curLine);
	}

	livePlay {|midiout, note_, chan_, stretch_ = 0.1|
		// manually control relative position
		this.midiout = midiout;


		this.pbs = 	Pbind (
			\type, \midi,
			\midiout, this.midiout,
	        [\dur,\degree], Pn(Plazy { Pseq(this.getCurrentFromPos()) }),
			\root, note_,
			\chan, chan_,
			\stretch, stretch_,
			\scale, Scale.chromatic

		).play;

		}
}
GradualMappingMouse : GradualMapping {
	*new { |startList, destList|
		^super.new(startList, destList).custom_init(); }
	custom_init {
		this.posInput = Bus.control;
		{Out.kr(posInput, MouseX.kr(0, 1))}.play;
		 }
	getPos {

		 ^this.posInput.getSynchronous;
	}


}

GradualMappingGUI : GradualMapping {
	// needs controller number for gui mapping
	var <> ctrlNum;
	classvar <> existingControllers;
	*new { |startList, destList, ctrlNum|

		^super.new(startList, destList).custom_init(ctrlNum); }
	custom_init { |ctrlNum|
		if (GradualMappingGUI.existingControllers == nil,
		{ GradualMappingGUI.existingControllers = 1 },
		{ GradualMappingGUI.existingControllers = GradualMappingGUI.existingControllers + 1 } );
		this.ctrlNum = GradualMappingGUI.existingControllers;
		this.initialiseOSC;
		this.posInput = 0;
	}
	*reset {
		GradualMappingGUI.existingControllers = nil;
	}
	initialiseOSC {
		// map to gui
		var name = ('gradMap/pos' ++ "0" ++ this.ctrlNum.asSymbol).asSymbol;
		OSCdef.new( ('\pos' ++ "0" ++ this.ctrlNum.asSymbol).asSymbol, { |msg|

			this.posInput = msg[1]; },
			name
		);
	}
	getPos {
		// this.posInput is updated each time slider on GUI is moved
		 ^this.posInput;
	}


}

GradualMapperGUISet {
	//basically GradMappingTransmitter without any of the OSC stuff that it was originally made for
	var <>gradMappers, <>master, <>stretch, <>midiout, <>presetView, <>presetButtonArray, <>h;
	*new { |length, stretch|
		^super.new.init(length,stretch) }
	init { |gradMappersDataArray, stretch|
		this.h = 50;
		MIDIClient.init;
		this.midiout = MIDIOut(0);
		this.master = Window("GradMapper", 1000@((1 + gradMappersDataArray.size)*h + 10))
		.front.alwaysOnTop_(true);
		this.master.onClose_({this.end()});
		this.master.view.decorator_(FlowLayout(this.master.bounds, 10@10, 10@10));
		this.makeButtons();
		this.gradMappers = Array.fill( gradMappersDataArray.size, {
			arg i;
			GradualMapping(gradMappersDataArray[i][0], gradMappersDataArray[i][1])
				.livePlay(this.midiout, 0, i, stretch)
				.createSliders(this);
		})


	}

	makeButtons {
		var w = 70;
		this.presetView = HLayoutView(this.master, 900@h);
		this.presetView.background(Color.red(0.6));
		this.presetButtonArray.add(Button(this.presetView, w@(h-10))
			.action_({this.setConfiguration([0.03,0.025,0.031,0.03,0.025,0.031])})
			.states_([["L-offset", Color(), Color.gray(0.9)]])
		);
		this.presetButtonArray.add(Button(this.presetView, w@(h-10))
			.action_({this.setConfiguration([0.03,0.98,0.031,0.03,0.97,0.031])})
			.states_([["Start", Color(), Color.gray(0.9)]])
		);
		this.presetButtonArray.add(Button(this.presetView, w@(h-10))
			.action_({this.setConfiguration([0.03,0.5,0.98,0.03,0.5,0.97])})
			.states_([["fanned", Color(), Color.gray(0.9)]])
		);
		this.presetButtonArray.add(Button(this.presetView, w@(h-10))
			.action_({this.setConfiguration([0.03,0.5,0.98,0.28,0.31,0.34])})
			.states_([["chiggersL", Color(), Color.gray(0.9)]])
		);
		this.presetButtonArray.add(Button(this.presetView, w@(h-10))
			.action_({this.setConfiguration([0.03,0.5,0.98,0.78,0.81,0.84])})
			.states_([["chiggersR", Color(), Color.gray(0.9)]])
		);
		this.presetButtonArray.add(Button(this.presetView, w@(h-10))
			.action_({this.setConfiguration([0.03,0.5,0.48,0.75,0.31,0.34])})
			.states_([["groove 4", Color(), Color.gray(0.9)]])
		);
		this.presetButtonArray.add(Button(this.presetView, w@(h-10))
			.action_({this.setConfiguration([0.99,0.5,0.48,0.75,0.31,0.34])})
			.states_([["groove 8 ", Color(), Color.gray(0.9)]])
		);
		this.presetButtonArray.add(Button(this.presetView, w@(h-10))
			.action_({this.setConfiguration([0.5,0.02,0.48,0.75,0.31,0.34])})
			.states_([["groove 4 inv ", Color(), Color.gray(0.9)]])
		);
		this.presetButtonArray.add(Button(this.presetView, w@(h-10))
			.action_({this.setConfiguration([0,0,0,1,1,1])})
			.states_([["4-10", Color(), Color.gray(0.9)]])
		);
		this.presetButtonArray.add(Button(this.presetView, w@(h-10))
			.action_({this.setConfiguration([1,1,1,0,0,0])})
			.states_([["5-8", Color(), Color.gray(0.9)]])
		);

	}

	end {
		this.gradMappers.do { |gm, i|
			gm.pbs.stop();
			this.midiout.allNotesOff(i);
		}
	}

	setConfiguration {|config|
		this.gradMappers.do { |gm, i|
			gm.setSlider(config[i]);
		}
	}



}

GradualMappingTransmitter {
	var <>gradMappers, <>length, <>barStretch, <>host, <>count, <>master;
	*new { |length, beatStretch|
		^super.new.init(length, beatStretch) }
	init { |length,beatStretch|
		this.length = length;

		this.barStretch = (length*beatStretch);
		// this.gradMappers = gradMappers;
		this.host = NetAddr("localhost", 4859);
		this.count = 0;
		/*this.initStart();
		this.initDest();*/
	}
	transmit {
		TempoClock.default.sched(0,{this.sendPatterns(this.getDataForTransmit(), "/current");
			(TempoClock.default.tempo*this.barStretch)} );
		// sendRoutine.play(TempoClock.default);


	}

	setGradMappersAndInit { |gradMappers|
		this.gradMappers = gradMappers;
		this.initStart();
		this.initDest();
		this.master = Window("GradMapper", 1000@220)
		.front.alwaysOnTop_(true);
		this.master.view.decorator_(FlowLayout(this.master.bounds, 10@10, 10@10));
		this.gradMappers.do { |gm|
			gm.createSliders(this);
		}
	}



	getDataForTransmit{
		var curArrays = [];
		this.count = this.count + 1;
		this.gradMappers.do {|gradMap|
			if (gradMap.automaticMode == false, {
			curArrays = curArrays.add(gradMap.getCurrentFromPos());
				["normal", gradMap.getCurrentFromPos()].postln;
				}, {
					if (count <= gradMap.gradList.size, {
						curArrays = curArrays.add(gradMap.gradList[this.count]);
						["special", gradMap.gradList[this.count]].postln;
						},
					{
						// finished
						}
						)
			})
		};
		^curArrays;
	}


	initStart {
		var startArrays = [];
		this.gradMappers.do {|gradMap|
			startArrays = startArrays.add(gradMap.startList);
		};
		this.sendPatterns(startArrays, "/startInit");
	}
	initDest {
		var destArrays = [];
		this.gradMappers.do {|gradMap|
			destArrays = destArrays.add(gradMap.destList);
		};
		this.sendPatterns(destArrays, "/destInit");
	}

	sendPatterns { |arrayOfPatterns, msgName|
		var result = [];
		arrayOfPatterns.do { |oneSet, gradMapId|
			10.do {|i|
				if (i <= (oneSet.size-1), {
					result = result.add(oneSet[i][0].asFloat/gradMappers[gradMapId].length);
				}, {result = result.add(-1.0);});
			}
		};
		"my result".postln;
		result.postln;
		this.host.sendMsg(msgName,
			result[0],
			result[1],
			result[2],
			result[3],
			result[4],
			result[5],
			result[6],
			result[7],
			result[8],
			result[9],
			result[10],
			result[11],
			result[12],
			result[13],
			result[14],
			result[15],
			result[16],
			result[17],
			result[18],
			result[19],
			result[20],
			result[21],
			result[22],
			result[23],
			result[24],
			result[25],
			result[26],
			result[27],
			result[28],
			result[29],
			result[30],
			result[31],
			result[32],
			result[33],
			result[34],
			result[35],
			result[36],
			result[37],
			result[38],
			result[39],

		);
	}

}
