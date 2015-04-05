L_system {
	var <>rules,<>derived_seqs, <>map, <>global_counter = 0;

	*new { |rules, axiom, map|
		^super.new.init(rules,axiom, map)
	}
	init { |rules, axiom = "a", map|
		this.map = Dictionary.new();
		if (rules == nil, {rules = Dictionary.newFrom(List["a","ab","b","a"])});
		if (map == nil, { rules.keys.asSortedList.do{|key, i|
			this.map.add(key.asString -> i);
			};
		});
		this.rules = rules;
		this.derived_seqs = List.new();
		this.format_axiom(axiom);

	}
	// convert axiom to [symbol, dur] format
	format_axiom { |axiom|
		var first_seq;
		first_seq = [];
		axiom.do { |symbol|	first_seq = first_seq.add([symbol, 1]) };
		this.derived_seqs = this.derived_seqs.add(first_seq);
	}

	// public function for doing derivations
	derive { |iterations = 1|
		iterations.do { this.derive_next() }
	}

	// private function, called by derive
	derive_next {
		var next_line;
		this.derived_seqs.wrapAt(-1).do { |prev_pair|
			var output_set;
			output_set = this.rules[prev_pair[0].asString];
			output_set.do { |output_symbol|
				next_line = next_line.add([output_symbol, (output_set.size * prev_pair[1])]);
			};
		};
		this.derived_seqs = this.derived_seqs.add(next_line.copy());
	}

	// for easy to read debugging output
	display {
		this.derived_seqs.do { |line|
			postln(line);
		}

	}
	play_single_seq { |line|
		var playable = [];
		line.do { |note|
			postln(note[0]);
			playable = playable.add([this.map[note[0].asString],1/note[1]]);
		};
		playable.postln;
		^playable;
	}

	play_group { |midiout, stretch = 2, scale = nil, start_line = 0, num_lines = 3, octaves = true, dif_channels = false|
		var pbs = [], channels = [], oct = [];
		if ((start_line + num_lines) > this.derived_seqs.size(),
			{ num_lines = (this.derived_seqs.size() - start_line) }
		);
		if (scale == nil, {scale = Scale.minorPentatonic});
		if (dif_channels,
			{ channels =  Array.fill(num_lines, { |i| i } ) },
			{ channels =  Array.fill(num_lines, { 0 } ) }
		);
		if (octaves,
			{   var start = -12, inc = 12;
				num_lines.do { oct = oct.add(start); start = start + inc}
			},
			{   oct =  Array.fill(num_lines, { 0 } ) }
		);
		pbs = Array.fill (num_lines,
			{ |i|
			Pbind (
					\type, \midi,
					\midiout, midiout,
					[\degree, \dur], Pseq(this.play_single_seq(this.derived_seqs[i+start_line].postln)),
					\chan, channels[i],
					\root, oct[i],
					\stretch, stretch,
					\scale, scale,
				).play;
		});

	}
	play_in_sequence { |midiout, stretch = 2, scale = nil, num_lines = 3, octaves = true, dif_channels = false , with_rests = true|
		var lines, num_segments, pbs = [], oct = [], channels = [], filename, midifile;
		num_segments = this.derived_seqs.size;
		if (scale == nil, {scale = Scale.minorPentatonic});
		if (dif_channels,
			{ channels =  Array.fill(num_lines, { |i| i } ) },
			{ channels =  Array.fill(num_lines, { 0 } ) }
		);
		if (octaves,
			{   var start = 12, inc = -12;
				num_lines.do { oct = oct.add(start); start = start + inc}
			},
			{   oct =  Array.fill(num_lines, { 0 } ) }
		);
	// build sequences:
		lines = Array.new(num_lines);
		num_lines.do { lines.add([])};
		num_segments.do { |seg|
			var seqs_in_seg, min = 0, cur_length = 0, padding = [];
			postln(["ON SEGMNET", seg]);
			if (seg >= num_lines, {min = (seg - (num_lines - 1))});
			seqs_in_seg = (seg..min);
			postln(["using lines", seqs_in_seg]);
			seqs_in_seg.do { |seq_num, i|
				var temp_seq;
				temp_seq = this.play_single_seq(this.derived_seqs[seq_num]);
				cur_length = 0;
				temp_seq.do { |tuple, j|
					// process the time value, maybe 2 depends on rule sizes
					temp_seq[j] = [tuple[0],(tuple[1] * (2**seg ))];
					cur_length = cur_length + temp_seq[j][1];
				};
			lines[i] = lines[i] ++ temp_seq;

			};
			padding = [];
			if ((seqs_in_seg.size < num_lines),
				{ padding = ((seg + 1)..(num_lines -1)) }
			);

			padding.do { |line_num|
				postln(["added rest in line_num", line_num,"in segment:", seg]);
				lines[line_num] = lines[line_num].add([\rest,cur_length]);
			};

			if (with_rests,
				{
					num_lines.do {|i|
					// add one beat to last value
						lines[i].wrapAt(-1)[1] = lines[i].wrapAt(-1)[1] + 4;
					lines[i] = lines[i].add([\rest, 4]) } } );
		};


		pbs = Array.fill (num_lines,
			{ |i|
			Pbind (
					\type, \midi,
					\midiout, midiout,
					[\degree, \dur], Pseq(lines[i].postln),
					\chan, channels[i],
					\root, oct[i],
					\stretch, stretch,
					\scale, scale,
				).play;
		});
		^lines;
	}


}
