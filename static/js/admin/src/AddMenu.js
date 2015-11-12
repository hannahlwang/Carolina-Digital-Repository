define('AddMenu', [ 'jquery', 'jquery-ui', 'underscore', 'CreateContainerForm', 'IngestPackageForm', 'CreateSimpleObjectForm', 'ImportMetadataXMLForm', 'IngestFromSourceForm', 'qtip'],
		function($, ui, _, CreateContainerForm, IngestPackageForm, CreateSimpleObjectForm, ImportMetadataXMLForm, IngestFromSourceForm) {
	
	function AddMenu(options) {
		this.options = $.extend({}, options);
		this.container = this.options.container;
		this.init();
	};
	
	AddMenu.prototype.getMenuItems = function() {
		var items = {};
		if ($.inArray('addRemoveContents', this.options.container.permissions) == -1)
			return items;
		items["addContainer"] = {name : "Add Container"};
		items["ingestPackage"] = {name : "Add Ingest Package"};
		items["ingestSource"] = {name : "Add from File Server"};
		items["simpleObject"] = {name : "Add Simple Object"};
		items["importMetadata"] = {name : "Import MODS"};
		return items;
	};
	
	AddMenu.prototype.setContainer = function(container) {
		this.container = container;
	};
	
	AddMenu.prototype.init = function() {
		var self = this;
		
		var items = self.getMenuItems();
		if ($.isEmptyObject(items))
			return;
		
		this.menu = $.contextMenu({
			selector: this.options.selector,
			trigger: 'left',
			className: 'add_to_container_menu', 
			events : {
				show: function() {
					this.addClass("active");
				},
				hide: function() {
					this.removeClass("active");
				}
			},
			items: items,
			callback : function(key, options) {
				switch (key) {
					case "addContainer" :
						new CreateContainerForm({
							alertHandler : self.options.alertHandler
						}).open(self.container.id);
						break;
					case "ingestPackage" :
						new IngestPackageForm({
							alertHandler : self.options.alertHandler
						}).open(self.container.id);
						break;
					case "ingestSource" :
						new IngestFromSourceForm({
							alertHandler : self.options.alertHandler
						}).open(self.container.id);
						break;
					case "simpleObject" :
						new CreateSimpleObjectForm({
							alertHandler : self.options.alertHandler
						}).open(self.container.id);
						break;
					case "importMetadata" :
						new ImportMetadataXMLForm({
							alertHandler : self.options.alertHandler
						}).open();
						break;
				}
			},
			position : function(options, x, y) {
				options.$menu.position({
					my : "right top",
					at : "right bottom",
					of : options.$trigger
				});
			}
		});
	};
	
	return AddMenu;
});