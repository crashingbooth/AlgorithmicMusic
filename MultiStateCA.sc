MultiStateCA {
	var <>cur_state, <>mod_factor, <>log, <>in_loop, <>mod_step,
	<>loop_len, <>remaining_reps, <>ceiling, <>direction;

	// call constructor
	*new { |cur_state, mod_factor = 2, mod_step = 1, ceiling = 10|
		^super.new.init(cur_state, mod_factor, mod_step, ceiling);
	}

	// constructor
	init { |cur_state, mod_factor, mod_step, ceiling|
		this.cur_state_(cur_state);
		this.mod_factor_(mod_factor);
		this.mod_step_(mod_step);
		this.log_(List[cur_state.copy]);
		this.in_loop_(false);
		this.loop_len(nil);
		this.remaining_reps(1);
		this.ceiling_(ceiling);
		this.direction_("up");
	}

	// derive new cur_state
	one_iteration {
		var len, result;
		len = this.cur_state.size;
		this.cur_state.do { |val, i |
			var next;
			next = this.cur_state[(i-1) % len]
			+ this.cur_state[i]
			+ this.cur_state[(i+1) % len];
			result = result.add(next % this.mod_factor);
		};
		result.do {|val, i|
			this.cur_state[i] = result[i];
		};
		// this.log_(this.log.add(this.cur_state.copy));
		^this.cur_state
	}

	detect_in_log {
		// only called if this.in_loop == false
		// linear search thru this.log, flag in_loop if found, else add to log
		this.log.do {|val, i|
			if (this.cur_state == val,
				{   this.in_loop_(true);
					this.loop_len_(this.log.size - i);
					// postln(["loop length is",this.loop_len]);
					case
						{this.loop_len < 4}  {this.remaining_reps_(4)}
						{this.loop_len == 4} {this.remaining_reps_(2)}
						{this.loop_len > 4}  {this.remaining_reps_(1)};
					// postln(["remaining rep is",this.remaining_reps]);
				}

			);
		};
		if (this.in_loop != true,
				{ this.log_(this.log.add(this.cur_state.copy)) }
		);

	}

	wade_thru_loop {
		// only called in this.in_loop == true
		// called if loop detected, check if final step, do bookkeeping, else do nothing
		var final = this.log[this.log.size - 1];
		// postln(["loop found, waiting for", final, " currently", this.cur_state]);
		if (this.cur_state == final,
			{   this.remaining_reps_(this.remaining_reps -1);
				if (this.remaining_reps == 0,
				{
						// this.mod_factor_(this.mod_factor + this.mod_step);
						this.update_mod();
						this.log_(List[this.cur_state.copy]);
						this.in_loop_(false);
						this.remaining_reps_(nil);
						this.loop_len_(nil);
					}
				);
			}
		);
	}
	update_mod {
		// take care of mod_factor, depending on direction, change direction if necessary
		// called by wade_thru_loop when loop is completed
		case
		{this.direction == "up"}
		    {
				this.mod_factor_(this.mod_factor + this.mod_step);
				if (this.mod_factor >= this.ceiling,
					{ this.direction_("down")}
				);
			}
		{this.direction == "down"}
		    {
				this.mod_factor_(this.mod_factor - this.mod_step);
				if (this.mod_factor <=  1,
					{ this.direction_("ending")}
			);
			}
		{this.direction == "ending"} { this.direction_("done")}
	}

	process {
		var result, prev;
		prev = this.cur_state.copy;
		result = this.make_playable();
		this.one_iteration;
		if (this.in_loop != true,
			{ this.detect_in_log() },
			{ this.wade_thru_loop() }
		);
		// postln(["in", prev]);
		postln(["out", this.cur_state]);
		postln(["mod", this.mod_factor, "dir", this.direction, "log len", this.log.size, "loop len", this.loop_len, "remain", this.remaining_reps]);
		if (this.direction == "done", {^nil}, {^result;})
		// ^result;
	}

	make_playable {
	// converts current CA array to degree,dur,amp tuples
	var result, prev = this.cur_state[0], dur = 0, amp = 0.2;
	this.cur_state.do {|val|
		if (val == prev,
			{   dur = dur +1 },
			{   result = result.add([prev, dur, amp]);
				prev = val;
				dur = 1;}
		);
	};
	result = result.add([prev, dur, amp]);
	result[0][2] = 0.4;
	^result
	}


} // end of class

