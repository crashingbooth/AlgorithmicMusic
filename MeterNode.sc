MeterNode {
	var <>parent, <>val, <>status,<>children, <>height;
	*new{ |val, children, parent, status, height|
		^super.new.init(val, children, parent, status)
	}

	init { |val, parent = nil,children = nil, status = nil, height = nil|
		this.val_(val); // a list of one or more beat lengths
		this.parent_(parent); // pointer to parent node
		if (this.children == nil, {this.children_([])});
		this.children_(children); // pointer to array of children node
		this.status_(status); // assume non-ternimal by default
		this.height = height;
		if (this.status == nil, {this.status_("n")});

	}

	modifyHeight { |modBy|
		this.height = this.height + modBy;
	}

	recursivelyModifyHeight { |modBy|
		// modify height of all descendants
		this.modifyHeight(modBy);
		this.children.do { |child|
			child.recursivelyModifyHeight(modBy);
		}
	}
}

MelodyNode : MeterNode {
	var <>note;
	*new{ |val, children, parent, status, height, note|
		^super.new(val, children, parent, status).sub_init(note)
	}

	sub_init { |note|
		this.note = note;
	}

	modifyNote { |modBy|

		this.note = this.note + modBy;
	}

	recursivelyModifyNote { |modBy|
		// changes note in all descendants by a factor of modBy
		this.modifyNote(modBy);
		this.children.do{ |child|
			child.recursivelyModifyNote(modBy);
		}
	}

	recursivelyAdjustSubtree { |modHeight = 0, modNote = 0|

		this.modifyHeight(modHeight);
		this.modifyNote(modNote);
		this.children.do { |child|
			child.recursivelyAdjustSubtree(modHeight, modNote);
		}

	}


}
