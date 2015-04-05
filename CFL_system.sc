
CFL_system {
	var <>rules, <>root, <>current_str, <>map;
	/**new {
		|rules|
		^super.new.rules_(rules = Dictionary.newFrom(List["a","ab","b","a"]))

	}*/
	// this is a normal constructor method
	*new { |rules, map|
		^super.new.init(rules, map)
	}
	init { |rules, map|
		//postln("rules", rules);
		var result = Dictionary.new();
		if (rules == nil, {rules = Dictionary.newFrom(List["a","ab","b","a"])});
		//postln("map", map == 7);
		if (map == nil, { rules.keys.asSortedList.do{|key, i|
			result.add(key.asString -> i);
		}; map = result
			});
		this.rules_(rules);
		this.map_(map);
	}
	gen_simple_map {
		var result = Dictionary.new();
		this.rules.keys.asSortedList.do{|key, i|
			result.add(key.asString -> i);
		};
		this.map_(result)
		}

	process_single {
		|char|
		var result;
		result = this.rules[char.asString];
		^result;
	}
	process {
		|str|
		var result;
		str.do {
			|char|
			result = result ++ this.process_single(char);
		}
		^result;
		}
	iterate {
	/* returns only final string */
		|str, iterations = 1|
		var result;
		iterations.do {
			result = this.process(str);
			str = result;
		};
		//this.current_str_(result);
		^result;
	}
	iterations {
	/* returns each iteration as element in list*/
		|str, cycles = 1|
		var results = Array.new(cycles), cur_result;
		postln(cycles);
		cycles.do {
			|i|
			postln(str);
			cur_result = this.process(str);
			results = results.add(cur_result);
			str = cur_result;

		};
		this.current_str_(results[cycles-1]);
		^results;
	}

	/*map_to_num {
		|str = "", mapping_arr = ""|
	// mapping_arr maps first rule key to 0
	var dict_size = this.rules.size, result = Array.new(str.size), map_dict = Dictionary.new();
		if (mapping_arr == "", {mapping_arr = Array.fill(this.rules.size, {|i|i})});
		if (str == "", {str = this.current_str});
	this.rules.keys.do { |key, i|
	map_dict.put(key.asString, mapping_arr[i]);
	};
	str.do { |char|
	result = result.add(map_dict[char.asString]);
	}
	^result
	}*/


}

