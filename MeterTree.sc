// METER Node and Tree

MeterTree {
	var <>root, <>surface, <>pattern_gen, <>tree_height, <>hierarchy, <>maximum_height, <>sortedNodeList;
	*new{ |seed, maximum_height = 4|
		// takes array of MeterNodes as input
		^super.new.init(seed, maximum_height) }

	init { |seed, maximum_height|
		var h = 0;
		this.init_dictionary();

		if (seed.isArray == false && seed.isInteger == false,
			{ // not array, not integer - lets assume it is a root node! 'cause hey!
				this.root_(seed); // and we're done
			},
			{
			// otherwise normal initialization
				if (seed.isInteger == true, {seed = [this.makeMeterNode(seed)]}); // convert int to MeterNode

				if (seed.size == 1,
					{ if (seed[0].isInteger, {seed[0] = this.makeMeterNode(seed[0])});
						this.root_(seed[0])},  // if seed has only one value, make it the root

					{if (seed[0].isInteger, {   // its an integer array, make it into Meternode array
						var new_seed = [];
						seed.do{|val|
							new_seed = new_seed.add(this.makeMeterNode(val));
						};
						seed = new_seed;
						});
						this.create_root(seed);
						h = h+1;
				});
				this.maximum_height = maximum_height;
				this.tree_height_(0);
				this.surface = [];
				postln(["seed", seed]);



				seed.do { |node|
					node.height_(h);
					this.expand(node)};
				this.stretch();
				this.sortedNodeList = SortedList(100, {|aNode, bNode|
					if (aNode[0] == bNode[0],
						{ aNode[1] > bNode[1] },
						{ aNode[0] < bNode[0] }
					)
				});
				this.build_hierarchy();
				// in the form [onset, length, theNode]



		});
	}
	init_dictionary {
			// initialize the dictionary
		this.pattern_gen_(Dictionary.new);
		this.pattern_gen[1] = [[1]];
		this.pattern_gen[2] = [[2],[1,1],[2], [2]];
		this.pattern_gen[3] = [[3],[3],[1,2],[1,1,1]];
		this.pattern_gen[4] = [[4],[4],[1,3],[2,2],[1,1,2],[1,1,1,1]];
		this.pattern_gen[5] = [[3,2]];
		this.pattern_gen[6] = [[3,3],[2,2,2]];
		this.pattern_gen[7] = [[3,4],[3,2,2]];
		this.pattern_gen[8] = [[4,4],[2,2,4],[3,3,2]];
		this.pattern_gen[9] = [[4,5],[3,3,3],[2,2,2,3]];
		this.pattern_gen[10]= [[5,5],[3,3,2,2],[4,6]];
		this.pattern_gen[11]= [[5,6],[4,3,2,2],[3,3,3,2]];
		this.pattern_gen[12]= [[3,3,3,3],[4,4,4],[8,4],[5,7],[6,6]];
		this.pattern_gen[13]= [[6,7],[5,8]];
		this.pattern_gen[14]= [[7,7],[8,6]];
		this.pattern_gen[15]= [[8,7],[5,5,5],[3,3,3,3,3]];
		this.pattern_gen[16]= [[4,4,4,4],[8,8],[10,6]];
		this.pattern_gen[22]= [[11,11]];
		this.pattern_gen[28]= [[14,14]];
		this.pattern_gen[32]= [[16,16]];
		this.pattern_gen[64]= [[32,32]];
	}
	create_root { |seed_array|
		var total_beats = 0, new_root;

	//	postln(["create_root", seed_array]);
		seed_array.do{ |node| total_beats = total_beats + node.val};
	//	postln(["toatal", total_beats]);
		new_root = this.makeMeterNode(total_beats, status:"n", height:0);
		new_root.children_(seed_array);
		seed_array.do{ |node| node.parent_(new_root)};
		this.root_(new_root);
	//	postln(["root", this.root.val]);


	}
	expand { |aNode|

		if (aNode.status.asString == "n" ,  // if it is a nonterminal node
			{
				var next_gen = this.pattern_gen[aNode.val].choose.scramble;  // get new pattern array
				aNode.children_([]);

				if ((next_gen[0] == aNode.val) || (aNode.height >= (this.maximum_height -1)),
					// if child is same as parent or tree is big enough
					{
						var terminal_node;

						terminal_node = this.makeMeterNode(aNode.val, aNode);
						terminal_node.parent_(aNode);
						terminal_node.status_("t");
						terminal_node.height_(aNode.height + 1);


						// shouldn't need this, but maybe I do:
						terminal_node.children_([]);
						aNode.children = aNode.children.add(terminal_node);
						this.surface = this.surface.add(terminal_node);


						if (terminal_node.height > this.tree_height, {this.tree_height_(terminal_node.height)})
						// done
					}, //else . . cycle through, create new nodes, recurse
					{
						next_gen.do { |new_val|

							aNode.children = aNode.children.add(this.makeMeterNode(new_val, aNode));
							aNode.children.wrapAt(-1).parent_(aNode);
							aNode.children.wrapAt(-1).status_("n");
							aNode.children.wrapAt(-1).height_(aNode.height+1);

						}
					}
				);
				aNode.children.do { |new_node| this.expand(new_node)}

			}
		);
	}

	stretch {
		this.surface.do {
			|aNode, i|
			while ( {aNode.height < this.tree_height } ,
				{
					var next_node;
					next_node = this.makeMeterNode(aNode.val);
					next_node.parent_(aNode);
					next_node.status_("t");
					next_node.height_(aNode.height + 1);
					aNode.children_([next_node]);
					aNode = next_node;
			});
			this.surface[i] = aNode;
		}
	}
	addToSortedNodeList { |aNode|
		// produced while doing build hierarchy
		var onset;
		if (aNode.height < this.maximum_height, { // exclude (redundant) final line
			onset = this.hierarchy[aNode.height].sum;
			this.sortedNodeList = this.sortedNodeList.add([onset, aNode.val, aNode])});
	}

	*findCommonNodes { |aTree, bTree|
		// finds all the nodes with the same onset time and lengths between the two trees
		var commonNodesList =  [], totalSize, aIndex = 0, bIndex= 0, aReducedNodeList,  bReducedNodeList;
		var count = 0;
		aReducedNodeList = MeterTree.removeNodeListRedundancy(aTree.sortedNodeList);
		bReducedNodeList = MeterTree.removeNodeListRedundancy(bTree.sortedNodeList);
		while ( {(aIndex < aReducedNodeList.size) && (bIndex < bReducedNodeList.size) && (count < 1000)},
			{
				count = count + 1;
				if (aReducedNodeList[aIndex][0] ==  bReducedNodeList[bIndex][0],
					{
						if (aReducedNodeList[aIndex][1] ==  bReducedNodeList[bIndex][1],
							{ commonNodesList = commonNodesList.add([aReducedNodeList[aIndex][2], bReducedNodeList[bIndex][2]]);
								// [aReducedNodeList[aIndex][0],aReducedNodeList[aIndex][1], ",", bReducedNodeList[bIndex][0],bReducedNodeList[bIndex][1]].postln;
							aIndex = aIndex + 1;
							bIndex = bIndex + 1;

							},
							{ if (aReducedNodeList[aIndex][1] < bReducedNodeList[bIndex][1],
								{aIndex = aIndex + 1}, {bIndex = bIndex + 1})});
					},
					{ if (aReducedNodeList[aIndex][0] < bReducedNodeList[bIndex][0],
						{aIndex = aIndex + 1}, {bIndex = bIndex + 1})});
		} );
		if (count > 900, {"infinite loop in MeterTree.findCommonNode".postln});
		^commonNodesList
	}
	*removeNodeListRedundancy {|nodeList|
		var prev = nil, reducedList = [];
		nodeList.do { |node, i|
			if (i > 0,
				{ if ((reducedList.wrapAt(-1)[0] == node[0]) && (reducedList.wrapAt(-1)[1] == node[1]),
					{ /*redundant: do nothing*/},
					{reducedList = reducedList.add(node) }
				)
				},
				{reducedList = reducedList.add(node)}
			);
		}
		^reducedList;
	}

	swapNodes { |aOldTree, bOldTree|
		var commonNodeList, nodePair, aOldParent, bOldParent, aSubBirthOrder, bSubBirthOrder, aTree, bTree;
		aTree = aOldTree.deepCopy;
		bTree = bOldTree.deepCopy;
		commonNodeList = MeterTree.findCommonNodes(aTree, bTree);
		if (commonNodeList.size == 1,
			{ "no usable subtrees".postln; ^[aTree, bTree] },
			{ nodePair = commonNodeList[rand(commonNodeList.size -1) + 1]; } //random choice excluding 0
		);

		commonNodeList.postln;
		aOldParent = nodePair[0].parent;
		bOldParent = nodePair[1].parent;

		// first find 'birth order of node to be replaced'
		aSubBirthOrder = aOldParent.children.indexOf(nodePair[0]);
		bSubBirthOrder = bOldParent.children.indexOf(nodePair[1]);

		// ["test before", aOldParent.children[aSubBirthOrder] == nodePair[0], aOldParent.children[aSubBirthOrder] == nodePair[1]].postln;
		// change parents to new children
		aOldParent.children[aSubBirthOrder] = nodePair[1];
		bOldParent.children[bSubBirthOrder] = nodePair[0];

		// change children (in nodePair) to new parents
		nodePair[0].parent = bOldParent;
		nodePair[1].parent = aOldParent;

		// ["test after", aOldParent.children[aSubBirthOrder] == nodePair[0], aOldParent.children[aSubBirthOrder] == nodePair[1]].postln;
		// modify heights of nodes in subtrees to match new parents
		nodePair.do { |node|
			this.fixSubtree(node)

		};

		// rebuild tree hierarchies
		aTree.find_surface();
		bTree.find_surface();
		aTree.build_hierarchy();
		bTree.build_hierarchy();
		^[aTree, bTree];


	}

	fixSubtree { |node|
		// called by swapNodes, fixes height of subtree, overrriden in MelodyTree
		var heightDiscrepancy;
			heightDiscrepancy = (node.parent.height + 1 - node.height );
			if (heightDiscrepancy != 0,
				{node.recursivelyModifyHeight(heightDiscrepancy )});
	}

	build_hierarchy {

		postln(["DEBUG tree_height",this.tree_height]);
		this.hierarchy_(Array.fill(this.tree_height + 1, {Array.fill(0)}));
		this.traverse_build(this.root);
	}

	traverse_build {
		// rebuilds hierarchy and sorted node list
		|aNode|
		var h = aNode.height;
		this.addToSortedNodeList(aNode);
		this.hierarchy[h] = this.hierarchy[h].add(aNode.val);
		if (aNode.children.size > 0, {
			aNode.children.do {
				|childNode|
				this.traverse_build(childNode);
			}
		})
	}
	make_playable_arrays {
		var result = Array.fill(this.tree_height + 1, {[]});
		this.hierarchy.do {|level, i|
			level.do { |length|
				length.do { |val, j|
					var next;
					if (j == 0, {next = 1}, {next = 0});
					result[i] = result[i].add(next);
				}
			}
		};
		^result;

	}

	deep_copy {
		var new_tree, new_root, new_children;
		// copy root, then recursively copy tree
		new_root = this.makeMeterNode(this.root.val);
		new_root.status_("n");
		new_root.children_([]);
		new_root.height_(0);
		this.root.children.do {
			|child|
			new_root.children = new_root.children.add(this.node_copy(child, new_root));
		};

		new_tree = MeterTree.new(new_root);
		new_tree.tree_height_(this.tree_height);
		new_tree.find_surface;
		new_tree.build_hierarchy;

		^new_tree

	}
	get_surface_vals {
		// returns array of durs for surface
		var result = [];
		this.surface.do { |node|
			result = result.add(node.val)
		};
		^result
	}

	node_copy {
		// recursively copies node structure
		|replacee, old_parent|
		var new_node, new_nodes_children;
		// postln(" node_copy got called");
		new_nodes_children = [];
		new_node = this.makeMeterNode(replacee.val, old_parent);
		new_node.parent_(old_parent);
		new_node.status_(replacee.status);
		new_node.height_(replacee.height);
		replacee.children.do {
			|child_node|
			new_nodes_children = new_nodes_children.add(this.node_copy(child_node, new_node));
		};
		new_node.children_(new_nodes_children);
		^new_node;
	}

	find_surface {
		// if it is copied tree, this will find the surface layer
		this.surface = [];
		this.root.children.do {
			|child|
			this.recursive_find_surface(child);
		}
	}

	recursive_find_surface {
		|aNode|
		if (aNode.height == this.tree_height,
			{
				this.surface = this.surface.add(aNode);
			},
			{
				aNode.children.do { |child| this.recursive_find_surface(child);	}
		})
	}

	create_variation {
		|depth = 0|
		var surface_node, anc_node, new_tree, new_anc, h;
		/*choose one node from the surface and find the first non-terminal ancestor
		- if depth > 1 find ancestor at depth
		then re-complete tree
		if tree is identical, trash it and try again, return alternate tree*/
		new_tree = this.deep_copy;  // acopy
		h = new_tree.tree_height;
		surface_node = new_tree.surface.choose;

		//find 1st anscestor
		anc_node = surface_node.parent;

		while ({anc_node.status != "n"} , { anc_node = anc_node.parent;});
		depth.do { if (anc_node.parent != nil , {anc_node = anc_node.parent; } )};
		postln(["max_h, anc_h:", this.tree_height, anc_node.height]);
		anc_node.children_([]); //prune
		new_tree.expand(anc_node);
		new_tree.stretch();
		new_tree.find_surface;
		new_tree.build_hierarchy;
		^new_tree;

	}
	*conjoin {
		|tree_array|
		var new_tree, new_root, child_list = [], total_length = 0, max_tree_height = 0;
		new_root = this.makeMeterNode.new();
		tree_array.do {
			|tree, i|

			total_length = total_length + tree.root.val;
			tree.root.parent_(new_root);
			child_list = child_list.add(tree.root);
			tree.root.recursivelyModifyHeight(1);
			// this.recursiveModHeight(tree.root, 1);
			postln([i, tree.tree_height]);
			if (tree.tree_height > max_tree_height, {max_tree_height = tree.tree_height});
		};
		postln("checkpoint2");
		new_root.val_(total_length);
		new_root.children_(child_list);
		new_root.height_(0);
		new_tree = MeterTree.new(new_root);
		new_tree.tree_height_(max_tree_height + 1);

		new_tree.stretch();
		new_tree.find_surface();
		new_tree.build_hierarchy();
		^new_tree

	}
	/**recursiveModHeight {
		|aNode, modBy|
		postln(["in rec_inc", aNode]);
		aNode.modifyHeight(modBy);
		aNode.children.do {
			|child|
			this.recursiveModHeight(child, modBy);
		}
	}*/
	show_structure {
		var arr;
		arr = this.make_playable_arrays();
		arr.do {
			|line, i|
			postln([i,line]);
		}
	}

	makeMeterNode { |val, parent = nil,children = nil, status = nil, height = nil|
		//overridden in melody version, but just creates new vanilla MeterNode here
		^MeterNode(val, parent,children, status, height);
	}

}

MelodyTree : MeterTree{

	makeMeterNode { |val, parent = nil,children = nil, status = nil, height = nil|
	//make MeterNode with note value
		var note;
		if (parent != nil, {
			note = parent.note + MelodyTree.gaussianRandom();}, {note = 0});
		^MelodyNode(val, parent,children, status, height, note: note);

	}
	*gaussianRandom { |range = 2| // -range < 0 < +range
			var sum = 0;
			5.do { sum = sum + ((range*2).rand - range) };
			^((sum/5).round(1));
	}

	gaussianConvolution { |atHeightFromBottom = 0|
		var descend = this.maximum_height - atHeightFromBottom;

		this.recursiveApplyConvolutionAtHeight(this.root, descend);
	}
	recursiveApplyConvolutionAtHeight { |node, targetHeight|
		var mod;
		["H", node.height].postln;
		if (node.height == targetHeight,
			{ mod = MelodyTree.gaussianRandom;
				node.recursivelyAdjustSubtree(modNote: mod);
			},
			{ node.children.do {|child| this.recursiveApplyConvolutionAtHeight(child, targetHeight) } }

		)
	}

	fixSubtree { |node|
		// called by swapNodes, fixes height and note of subtree (overrides method in MeterNode)
		// note values of subtree adjusted to be relative to parent node
		var heightDiscrepancy, noteDiscrepancy;
		heightDiscrepancy = (node.parent.height + 1 - node.height );
		noteDiscrepancy = node.parent.note;
		node.recursivelyAdjustSubtree(heightDiscrepancy, noteDiscrepancy);

	}

	make_playable_arrays {
		var playable = [];
		this.surface.do { |node|
			playable = playable.add([node.note, node.val]);
		};
		^playable;

	}
	simplePlay { |rep = 1, pan = 0|
		var melody = this.make_playable_arrays;
		Pbind (
			[\degree, \dur], Pseq(melody, rep),
			\scale,  Scale.dorian,
			\stretch, 0.2,
			\pan, pan
		).play
	}

	deep_copy {
		var new_tree, new_root, new_children;
		// copy root, then recursively copy tree
		new_root = this.makeMeterNode(this.root.val);
		new_root.status_("n");
		new_root.children_([]);
		new_root.height_(0);
		this.root.children.do {
			|child|
			new_root.children = new_root.children.add(this.node_copy(child, new_root));
		};

		new_tree = MelodyTree.new(new_root,this.maximum_height);
		new_tree.tree_height_(this.tree_height);
		new_tree.find_surface;
		new_tree.build_hierarchy;

		^new_tree

	}
	node_copy {
		// recursively copies node structure
		|replacee, old_parent|
		var new_node, new_nodes_children;
		// postln(" node_copy got called");
		new_nodes_children = [];
		new_node = this.makeMeterNode(replacee.val, old_parent);
		new_node.parent_(old_parent);
		new_node.status_(replacee.status);
		new_node.height_(replacee.height);
		new_node.note = replacee.note;  // all else here redundant code, maybe refactor
		replacee.children.do {
			|child_node|
			new_nodes_children = new_nodes_children.add(this.node_copy(child_node, new_node));
		};
		new_node.children_(new_nodes_children);
		^new_node;
	}


}

		