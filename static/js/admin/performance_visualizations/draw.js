/**
 * Draw graphs
 */

CdrGraphs.prototype.draw = function() {
    /**
     * Basic setup & data manipulation
     *
     *
     */
    var self = this;
    var width = window.innerWidth - this.margins.left - this.margins.right;
    var parseDate = d3.time.format("%Y-%m-%d").parse;

    // Fields where metrics are by day only (e.g.) older dates & operations metrics
    this.operations.forEach(function(d) {
        d.date = (typeof d.date === "object") ? d.date : parseDate(d.date);
        d.throughput_bytes = (d.throughput_bytes === "") ? 0 : d.throughput_bytes / 1000000;
        d.throughput_files = (d.throughput_files === "") ? 0 : +d.throughput_files;
        d.moves = (d.moves === "") ? 0 : +d.moves;
        d.finished_enhancements = (d.finished_enhancements === "") ? 0 : +d.finished_enhancements;
        d.failed_enhancements = (d.failed_enhancements === "") ? 0 : +d.failed_enhancements;
    });
    
    var sorted_operations = this.dateSort(this.operations);
    var data = this.dataFilter(sorted_operations, "throughput_bytes");
    
    // Metrics by uuid & day, newer deposit date metrics
    this.deposits.forEach(function(d) {
        d.date = (typeof d.date === "object") ? d.date : parseDate(d.date);
        d.ingest_duration = d.ingest_duration / 1000;
        d.queued_duration = d.queued_duration / 1000;
        d.total_time = d.ingest_duration + d.queued_duration;
        d.throughput_bytes = (d.throughput_bytes === "") ? 0 : d.throughput_bytes / 1000000;
        d.throughput_files = (d.throughput_files === "") ? 0 : +d.throughput_files;
    });
    
    var deposits_by_uuid = this.dateSort(this.deposits);

    var height_range = [0, this.height];

    // xScale for most plots
    var xScale = this.xScales(data, width);

    // Get totals data for charts for uuid metrics & add merge it with totals data from operations metrics
    var throughput_bytes_uuid_totals = this.counts(this.deposits, "throughput_bytes");
    var throughput_files_uuid_totals = this.counts(this.deposits, "throughput_files");

    var throughput_combined = this.combined(throughput_bytes_uuid_totals, throughput_files_uuid_totals);
    var throughput_all = this.combined(data, throughput_combined);

    // Duration totals by day
    var combine_deposits_duration = this.combined(
        this.counts(deposits_by_uuid, "ingest_duration"),
        this.counts(deposits_by_uuid, "queued_duration")
    );

    var duration_all = this.combined(
        combine_deposits_duration,
        this.counts(deposits_by_uuid, "total_time")
    );


    /**
     *  Scatter plot & Strip plot - megabytes by date
     *
     *
     **/
    var throughput = "throughput_bytes";
    var yScale = this.yScales(throughput_all, throughput, height_range);

    var xAxis = this.getAxis(xScale, "bottom");
    var yAxis = this.getAxis(yScale, "left");

    var throughput_date = this.showAxises("#throughput-date", xAxis, yAxis, width, "Throughput (MB)");
    this.drawCircles(throughput_date, throughput_all, xScale, yScale, throughput);
    focusHover(throughput_date, throughput_all, "#throughput-date");

    this.drawLegend("#throughput-legend", throughput_all, throughput);
    drawStrip("#throughput-date-strip", throughput_all, throughput);
    
    // By uuid
    var xScaleDeposits = this.xScales(deposits_by_uuid, width);
    var yScaleDeposits = this.yScales(deposits_by_uuid, throughput, height_range);

    var xAxisDeposits = this.getAxis(xScaleDeposits, "bottom");
    var yAxisDeposits  = this.getAxis(yScaleDeposits, "left");

    var throughput_uuid = this.showAxises("#throughput-deposit", xAxisDeposits , yAxisDeposits , width, "Throughput (MB)");
    this.drawCircles(throughput_uuid, deposits_by_uuid, xScaleDeposits, yScaleDeposits, throughput);


    /**
     * Deposit Duration
     *
     *
     */
    
    // duration totals by deposit by day
    var total_time = "total_time";
    var yScaleTotal = this.yScales(deposits_by_uuid, total_time, height_range);
    var xAxisDuration = this.getAxis(xScaleDeposits, "bottom");
    var yAxisDuration = this.getAxis(yScaleTotal, "left");
    var duration_date = this.showAxises("#duration-date", xAxisDuration, yAxisDuration, width, "Time (Seconds)");

    this.drawCircles(duration_date, deposits_by_uuid, xScaleDeposits, yScaleTotal, total_time);
    this.durationChartUpdate(deposits_by_uuid, xScaleDeposits, yScaleTotal, yAxisDuration);

    var yScaleTotalDay = this.yScales(duration_all, total_time, height_range);
    var xAxisTotal = this.getAxis(xScaleDeposits, "bottom");
    var yAxisTotal = this.getAxis(yScaleTotalDay, "left");
    var all_duration_date = this.showAxises("#duration-total-date", xAxisTotal, yAxisTotal, width, "Time (Seconds)");

    this.drawCircles(all_duration_date, duration_all, xScaleDeposits, yScaleTotalDay, total_time);
    this.durationChartUpdate(duration_all, xScaleDeposits, yScaleTotalDay, yAxisTotal);

    /** Don't think this will work here
   // this.drawLegend("#duration-total-legend", duration_all, "total_time");
    //drawStrip("#duration-total-strip", duration_all, "total_time");

    /**
     *  Scatter plot & Strip plot - moves by date
     *
     *
     **/

    var moves = "moves";
    var yScaleMoves = this.yScales(data, moves, height_range);
    var yAxisMoves = this.getAxis(yScaleMoves, "left");
    var moves_date = this.showAxises("#moves-date", xAxis, yAxisMoves, width, "Move Operations");
    this.drawCircles(moves_date,  data, xScale, yScaleMoves, moves);
    focusHover(moves_date, data, "#moves-date");

    this.drawLegend("#moves-legend", data, moves);
    drawStrip("#moves-date-strip", data, moves);

    /**
     * Scatter plot & Strip plot - enhancements by date
     */

    var finished_enh = "finished_enhancements";
    var yScaleFinishedEnh = this.yScales(data, finished_enh, height_range);
    var yAxis_finished_enh = this.getAxis(yScaleFinishedEnh, "left");
    var finished_enh_date = this.showAxises("#enh-date", xAxis, yAxis_finished_enh, width, "Finished Enhancements");
    this.drawCircles(finished_enh_date,  data, xScale, yScaleFinishedEnh, finished_enh);
    focusHover(finished_enh_date, data, "#enh-date");

    this.drawLegend("#enh-legend", data, finished_enh);
    drawStrip("#enh-date-strip", data, finished_enh);

    /**
     * Scatter plot & Strip plot - failed enhancements by date
     */

    var failed_enh = "failed_enhancements";
    var yScaleFailedEnh = this.yScales(data, failed_enh, height_range);
    var yAxis_failed_enh = this.getAxis(yScaleFailedEnh, "left");
    var failed_enh_date = this.showAxises("#failed-enh-date", xAxis, yAxis_failed_enh, width, "Failed Enhancements");
    this.drawCircles(failed_enh_date,  data, xScale, yScaleFailedEnh, failed_enh);
    focusHover(failed_enh_date, data, "#failed-enh-date");

    this.drawLegend("#failed-enh-legend", data, failed_enh);
    drawStrip("#failed-enh-date-strip", data, failed_enh);

    // Make graphs visible
    this.hideShow();


    /**
     * Draw strip chart
     * @param selector
     * @param data
     * @param field
     * @returns {*}
     */
    function drawStrip(selector, data, field) {
        var strip_color = self.stripColors(data, field);
        var bar_width = self.barWidth(width, self.operations);
        var tip = d3.tip().attr("class", "d3-tip").html(function(d) {
            return self.tipTextOperations(d);
        });

        var strip = d3.select(selector)
            .attr("width", width + self.margins.left + self.margins.right)
            .attr("height", 110)
            .call(tip);

        var add = strip.selectAll("bar")
            .data(data);

        add.enter().append("rect");

        add.attr("x", function(d) { return xScale(d.date); })
            .attr("width", bar_width)
            .attr("y", 0)
            .attr("height", 80)
            .translate([self.margins.left, 0])
            .style("fill", function(d) { return strip_color(d[field]); })
            .on("mouseover", function(d) {
                d3.select(this).attr("height", 100);
                tip.show.call(this, d);
            })
            .on("mouseout", function(d) {
                d3.select(this).attr("height", 80);
                tip.hide.call(this, d);
            });

        add.exit().remove();

        return add;
    }

    /**
     * Add overlay line & text
     * @param chart
     * @param data
     * @param selector
     * @returns {*}
     */
    function focusHover(chart, data, selector) {
        var margins = self.margins;
        var bisectDate = d3.bisector(d3.f("date")).right;

        var focus = chart.append("g")
            .attr("class", "focus")
            .style("display", "none");

        focus.append("line")
            .attr("class", "y0")
            .attr({
                x1: 0,
                y1: 0,
                x2: 0,
                y2: self.height

            });

        chart.append("rect")
            .attr("class", "overlay")
            .attr("width", width)
            .attr("height", self.height)
            .on("mouseover touchstart", function() { focus.style("display", null); })
            .on("mouseout touchend", function() {
                focus.style("display", "none");
                self.scatter_tip.transition()
                    .duration(250)
                    .style("opacity", 0);
            })
            .on("mousemove touchmove", mousemove)
            .translate([margins.left, margins.top]);

        function mousemove() {
            var x0 = xScale.invert(d3.mouse(this)[0]),
                i = bisectDate(data, x0, 1),
                d0 = data[i - 1],
                d1 = data[i];

            if(d1 === undefined) d1 = Infinity;
            var d = x0 - d0.key > d1.key - x0 ? d1 : d0;

            var transform_values = [(xScale(d.date) + margins.left), margins.top];
            d3.select(selector + " line.y0").translate(transform_values);

            self.scatter_tip.transition()
                .duration(100)
                .style("opacity", .9);

            self.scatter_tip.html(self.tipTextOperations(d))
                .style("top", (d3.event.pageY-28)+"px")
                .style("left", (d3.event.pageX-158)+"px");
        }

        return chart;
    }
};