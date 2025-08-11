function NewProductsView(newProductsController, newProductsModel, viewId) {
	this._newProductsController = newProductsController;
	this._newProductsModel = newProductsModel;
	this._viewId = viewId;
	
	this._$newProductsTableBody = $("<tbody>");
	this.rowIndex = 0;

	var _refreshableFields = [];

    this.refresh = function() {
        for(var field of _refreshableFields) {
            field.refresh();
        }
    }
	
	this.repaint = function($container, spaceCode) {
		var _this = this;
		$container.empty();
		
		var $newProducts = $("<div>");
			$newProducts.append($("<legend>").append("Create and add new product"));
			
		var $newProductsTable = $("<table>", { class : "table table-bordered" });
		var $newProductsTableHead = $("<thead>");
		var $newProductsTableHeaders = $("<tr>")
											.append($("<th>").append("Name"))
											.append($("<th>").append("Catalog Num"))
											.append($("<th>").append("Price"))
											.append($("<th>").append("Currency"))
											.append($("<th>").append("Supplier"))
											.append($("<th>").append("Quantity"))
											.append($("<th>").append(FormUtil.getButtonWithIcon("glyphicon-plus", function() {
												_this.addNewProduct(_this._$newProductsTableBody, spaceCode);
												_this.rowIndex++;
											}, null, null, "add-new-product-btn-"+_this._viewId)));
		
		$newProductsTable.append($newProductsTableHead.append($newProductsTableHeaders)).append(this._$newProductsTableBody);
		$newProducts.append($newProductsTable);
			
		$container.append($newProducts);
	}
	
	this.addNewProduct = function($newProductsTableBody, spaceCode) {
	    var _this = this;
		mainController.serverFacade.searchWithType("SUPPLIER", null, false, function(suppliers){
			var supplierTerms = [];
			for(var sIdx = 0; sIdx < suppliers.length; sIdx++) {
                var supplier = suppliers[sIdx];
                if (supplier.spaceCode == spaceCode) {
                    var name = supplier.properties[profile.getInternalNamespacePrefix() + "NAME"];
                    if (!name) {
                        name = supplier.properties["NAME"];
                    }
                    if (!name) {
                        name = supplier.code;
                    }
                    supplierTerms.push({code : supplier.identifier, label : name });
                }
			}
			var supplierDropdown = FormUtil.getDropDownForTerms("new-product-supplier-" + _this._viewId + "-" + _this.rowIndex, supplierTerms, "Select a supplier", true);
			_refreshableFields.push(supplierDropdown);

			var currencyVocabulary = profile.getVocabularyByCode(profile.getInternalNamespacePrefix() + "PRODUCT.CURRENCY");
			var currencyDropdown = FormUtil.getDropDownForTerms("new-product-currency-" + _this._viewId + "-" + _this.rowIndex, currencyVocabulary.terms, "Select a currency", false);
			_refreshableFields.push(currencyDropdown);

			var quantityFieldId = "new-product-quantity-" + _this._viewId + "-" + _this.rowIndex;
			var quantityField = FormUtil.getIntegerInputField(quantityFieldId, "Quantity", true);
			var quantityChangeEvent = function() {
                var value = $(this).val();
                try {
                    var valueParsed = parseInt(value);
                    if("" + valueParsed === "NaN") {
                        Util.showUserError("Please input a correct quantity.");
                        $(this).val("");
                    } else {
                        $(this).val(valueParsed);
                    }
                } catch(err) {
                    Util.showUserError("Please input a correct quantity.");
                    $(this).val("");
                }
            }
            quantityField.change(quantityChangeEvent);
            quantityField.refresh = function() {
                this.unbind();
                this.change(quantityChangeEvent);
            }
            _refreshableFields.push(quantityField);

			var priceFieldId = "new-product-price-" + _this._viewId + "-" + _this.rowIndex;
			var priceField = FormUtil.getRealInputField(priceFieldId, "Price", false);
			var priceChangeEvent = function() {
                var value = $(this).val();
                if(value) {
                    try {
                        var valueParsed = parseFloat(value);
                        if("" + valueParsed === "NaN") {
                            Util.showUserError("Please input a correct price.");
                            $(this).val("");
                        } else {
                            $(this).val(valueParsed);
                        }
                    } catch(err) {
                        Util.showUserError("Please input a correct price.");
                        $(this).val("");
                    }
                }
            }
            priceField.change(priceChangeEvent);
            priceField.refresh = function() {
                this.unbind();
                this.change(priceChangeEvent);
            }
            _refreshableFields.push(priceField);

			var priceFieldId = "new-catalog-number-" + _this._viewId + "-" + _this.rowIndex;
			var catalogNumField = FormUtil.getTextInputField(null, "Catalog Num", true);
			
			var nameField = FormUtil.getTextInputField("new-product-name-" + _this._viewId + "-" + _this.rowIndex, "Name", true);
			var tableRowId = "table-row-id-" + _this._viewId + "-" + _this.rowIndex;
			var $newProductsTableRow = $("<tr>")
			.append($("<td>").append(nameField))
			.append($("<td>").append(catalogNumField))
			.append($("<td>").append(priceField))
			.append($("<td>").append(currencyDropdown))
			.append($("<td>").append(supplierDropdown))
			.append($("<td>").append(quantityField))
			.append($("<td>").append(FormUtil.getButtonWithIcon("glyphicon-minus", function() {
				$(this).parent().parent().remove();
			}, null, null, tableRowId)));
			
			$newProductsTableBody.append($newProductsTableRow);
		});
		
		
		
	}
	
}