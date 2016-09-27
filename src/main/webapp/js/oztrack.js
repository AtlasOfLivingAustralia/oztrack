/*global confirm, alert*/
(function(OzTrack) {
    OzTrack.serializeHash = function(selector) {
        var params = [];
        $.each($(selector).serializeArray(), function(i, pair) {
            if ($.isArray(params[pair.name])) {
                params[pair.name].push(pair.value);
            }
            else if (params[pair.name]) {
                var a = [];
                a.push(params[pair.name]);
                a.push(pair.value);
                params[pair.name] = a;
            }
            else {
                params[pair.name] = pair.value;
            }
        });
        return params;
    };

    OzTrack.deleteEntity = function(url, destUrl, message) {
        if (!confirm(message)) {
            return;
        }
        var loadingOverlay = $('<div class="loadingOverlay">').appendTo($('body'));
        $.ajax({
            url: url,
            type: 'POST',
            data: {
                '_method': 'DELETE'
            },
            error: function(xhr, textStatus, errorThrown) {
                loadingOverlay.remove();
                alert('Error processing delete');
            },
            success: function (data,textStatus, jqXHR) {
                window.location = destUrl;
            }
        });
    };

    OzTrack.initHelpPopover = function(helpPopover) {
        $('<a class="help-popover-icon" href="javascript:void(0);">')
            .insertBefore(helpPopover)
            .popover({
                container: 'body',
                placement: 'right',
                trigger: 'click',
                html: true,
                title: helpPopover.attr('title'),
                content: helpPopover.html()
            });
    };
}(window.OzTrack = window.OzTrack || {}));

$.datepicker.setDefaults({
    dateFormat: 'yy-mm-dd',
    altFormat: 'yy-mm-dd',
    changeMonth: true,
    changeYear: true,
    firstDay: 1 // make first day of week Monday (default is Sunday)
});

// Render HTML in jQuery autocomplete results
//
// Taken from code by Scott González:
// https://github.com/scottgonzalez/jquery-ui-extensions/blob/master/autocomplete/jquery.ui.autocomplete.html.js
$.ui.autocomplete.prototype._renderItem = function( ul, item) {
    return $('<li></li>')
        .data('item.autocomplete', item)
        .append($('<a></a>').html(item.label))
        .appendTo(ul);
};

$(document).ready(function() {
    // Fix bug where clearing field doesn't clear alt field
    // http://bugs.jqueryui.com/ticket/5734
    // http://stackoverflow.com/questions/3922592/jquery-ui-datepicker-clearing-the-altfield-when-the-primary-field-is-cleared
    $('.datepicker').change(function() {
        var altField = $(this).datepicker('option', 'altField');
        if (altField && !$(this).val()) {
            $(altField).val('');
        }
    });

    $('.help-popover').each(function() {OzTrack.initHelpPopover($(this));});

    $('.control-group.required').find('label:first').append($('<i class="required-marker">*</i>'));
});

$(document).click(function(e) {
    // Hide popovers unless: clicking on one, because it might contain interactive elements;
    if ($(e.target).closest('.popover').length !== 0) {
        return;
    }
    // or clicking on a popover icon, in which case we rely in its natural show/hide behaviour. 
    var popoversToHide = $('.help-popover-icon,.layer-opacity-popover-icon').filter(function(i) {
        return this !== e.target;
    });
    popoversToHide.popover('hide');
});

var showChar = 50;
var ellipsestext = "...";
var moretext = "more";
var lesstext = "less";

$('.more').each(function() {
    var content = $(this).html();
    if(content.length > showChar) {

        var c = content.substr(0, showChar);
        var h = content.substr(showChar, content.length - showChar);

        var html = c + '<span class="moreellipses">' + ellipsestext + ' </span>' +
            '<span class="morecontent"><span>' + h +
            '</span>  <a href="" class="morelink">' + moretext + '</a></span>';

        $(this).html(html);
    }
});

$(".morelink").click(function(){
    if($(this).hasClass("less")) {
        $(this).removeClass("less");
        $(this).html(moretext);
    } else {
        $(this).addClass("less");
        $(this).html(lesstext);
    }
    $(this).parent().prev().toggle();
    $(this).prev().toggle();
    return false;
});
