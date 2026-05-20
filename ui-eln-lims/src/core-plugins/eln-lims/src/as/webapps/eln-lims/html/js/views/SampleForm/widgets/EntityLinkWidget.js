var EntityLinkWidget = new function()
{

    this.getEntityLinkDropdown = function(mode, sample, entityName) {
        if(mode !== FormMode.VIEW) {
            return null;
        }
        //
        return {
            // label - label in dropdown
            label: "Link entity to "+entityName,
            // title - tooltip
            title: "Link entity to "+entityName,
            action: function () {
                Util.blockUINoMessage();
                var component = $("<div>");
                component.append($("<legend>", { 'text' : "Link entity to "+entityName}))
                component.append($('<br>'));

                var $inputURL = $('<input>', {'type' : 'url', 'id' : 'input-logbook-url-' + mainController.getNextId(), 'alt' : entityName+' url', 'placeholder' : entityName+' url goes here', 'class' : 'form-control'});
                var value = '';
                if(sample && sample.metaData['ENTITY_LINK.URL']) {
                    value = sample.metaData['ENTITY_LINK.URL'];
                }
                $inputURL.val(value)
                var url = value;
                var keyupFunction = function(event){
                    var textField = $(this);
                    url = textField.val();
                };
                $inputURL.keyup(keyupFunction);

                var inputGroup = FormUtil.getFieldForComponentWithLabel($inputURL, 'URL');
                component.append(inputGroup);

                component.append($('<br>'));
                var acceptBtn = $("<a>", { 'class' : 'btn btn-primary', 'id' : 'updateAccept', 'text' : 'Accept' });
                var closeBtn = $("<a>", { 'class' : 'btn btn-default', 'id' : 'updateCancel', 'text' : 'Cancel' });
                component.append(acceptBtn).append('&nbsp;').append(closeBtn);

                Util.blockUI(component, FormUtil.getDialogCss());

                $("#updateAccept").on("click", function(event) {
                    require([
                            "as/dto/sample/update/SampleUpdate",
                            "as/dto/sample/id/SamplePermId"],
                        function (SampleUpdate, SamplePermId
                        ) {
                            const update = new SampleUpdate();
                            update.setSampleId(new SamplePermId(sample.permId.permId));
                            update.getMetaData().put('ENTITY_LINK.URL', url);

                            mainController.openbisV3.updateSamples([update]).done(function(x) {
                                let message = url ? "Entity linked" : "Entity unlinked";
                                mainController.refreshView();
                                Util.showSuccess(message, function () { Util.unblockUI(); });
                            }).fail(function(err) {
                                Util.showError(err.message, function() {}, true);
                            });

                        }
                    );
                });


                $("#updateCancel").on("click", function(event) {
                    Util.unblockUI();
                });

            }
        }
    }

    this.getEntityLinkContainer = function($container, model) {
        if (model.mode === FormMode.VIEW &&
            model.sample.sampleTypeCode === "LOGBOOK" &&
            model.v3_sample.metaData['ENTITY_LINK.URL']) {

            var id = mainController.getNextId();
            var $div = $("<div>", {'id': 'iframe-container'});
            $div.css({
                'position': 'relative',
                'width': '100%'
            })
            var frameId = 'linked-frame-' + id;
            var $frame = $("<iframe>", {'src': model.v3_sample.metaData['ENTITY_LINK.URL'], 'id': frameId});
            $frame.css({
                'width': '100%',
                // 'height': '100%',
                'height': '500px',
                'display': 'block',
                'border': '1px solid #ccc'
            });

            var resizerId = 'resizer-' + id;
            var $resizer = $("<div>", {'id': resizerId});
            $resizer.css({
                'width': '100%',
                'height': '8px',
                'background': '#ddd',
                'cursor': 'ns-resize',
                'display': 'flex',
                'align-items': 'center',
                'justify-content': 'center'
            });

            $div.append($frame);

            $div.append($resizer);
            $container.append($div);


            let isResizing = false;
            let startY, startHeight;

            var dragFunction = function (e) {
                isResizing = true;
                startY = e.clientY;
                startHeight = $('#' + frameId).outerHeight();
                $('body').css('cursor', 'ns-resize');
                $('#' + frameId).css('pointer-events', 'none');
            };

            $resizer.mousedown(dragFunction);
            $resizer.refresh = function () {
                $resizer.off('mousedown');
                $resizer.mousedown(dragFunction);
            }

            $(document).on('mousemove', function (e) {
                if (!isResizing) return;
                const newHeight = Math.max(100, startHeight + (e.clientY - startY));
                $('#' + frameId).css('height', newHeight + 'px');
            });

            $(document).on('mouseup', function () {
                if (!isResizing) return;
                isResizing = false;
                $('body').css('cursor', '');
                $('#' + frameId).css('pointer-events', 'auto');
            });

            return $resizer;
        }
    }
}