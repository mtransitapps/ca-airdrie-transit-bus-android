package org.mtransit.parser.ca_airdrie_transit_bus;

import static org.mtransit.commons.Constants.EMPTY;

import org.jetbrains.annotations.NotNull;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://data-airdrie.opendata.arcgis.com/datasets/airdrie-transit-bus-routes/about
// https://www.airdrie.ca/gettransitgtfs.cfm
// TODO real-time https://airdrietransit.transloc.com/
public class AirdrieTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new AirdrieTransitBusAgencyTools().start(args);
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "Airdrie Transit";
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("\\d+");

	private static final long RID_ENDS_WITH_AM = 10_000L;
	private static final long RID_ENDS_WITH_PM = 20_000L;

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		final String rsn = gRoute.getRouteShortName();
		if (!CharUtils.isDigitsOnly(rsn)) {
			final String rsnLC = rsn.toLowerCase(Locale.ENGLISH);
			Matcher matcher = DIGITS.matcher(rsn);
			if (matcher.find()) {
				int digits = Integer.parseInt(matcher.group());
				if (rsnLC.endsWith("am")) {
					return digits + RID_ENDS_WITH_AM;
				} else if (rsnLC.endsWith("pm")) {
					return digits + RID_ENDS_WITH_PM;
				}
			}
			if ("downtown ice service".equals(rsnLC)) {
				String rlnLC = gRoute.getRouteLongNameOrDefault().toLowerCase(Locale.ENGLISH);
				if (rlnLC.endsWith("morning")) {
					return 900 + RID_ENDS_WITH_AM;
				} else if (rlnLC.endsWith("afternoon")) {
					return 900 + RID_ENDS_WITH_PM;
				}
			}
			throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute);
		}
		return Long.parseLong(rsn);
	}

	@NotNull
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		final String rsn = gRoute.getRouteShortName();
		final String rsnLC = rsn.toLowerCase(Locale.ENGLISH);
		if ("downtown ice service".equals(rsnLC)) {
			String rlnLC = gRoute.getRouteLongNameOrDefault().toLowerCase(Locale.ENGLISH);
			if (rlnLC.endsWith("morning")) {
				return "D ICE AM";
			} else if (rlnLC.endsWith("afternoon")) {
				return "D ICE PM";
			}
		}
		return super.getRouteShortName(gRoute);
	}

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	private static final String AGENCY_COLOR_BLUE = "0099CC"; // BLUE (from web site CSS)
	// private static final String AGENCY_COLOR_BLUE_DARK = "003399"; // BLUE DARK (from web site CSS)

	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final Pattern INBOUND_OUTBOUND_ = CleanUtils.cleanWords("inbound", "outbound");

	private static final Pattern TRANSIT_TERMINAL_ = CleanUtils.cleanWords("transit terminal");
	private static final String TRANSIT_TERMINAL_REPLACEMENT = CleanUtils.cleanWordsReplacement("Term");

	private static final Pattern STARTS_WITH_BOUNDS = Pattern.compile("(^(eb|nb|sb|wb) )", Pattern.CASE_INSENSITIVE);

	private static final Pattern KEEP_INSIDE_PARENTHESES = Pattern.compile("(([^(]+)\\(([^)]+)\\))", Pattern.CASE_INSENSITIVE);
	private static final String KEEP_INSIDE_PARENTHESES_REPLACEMENT = "$3";

	private static final Pattern ENDS_WITH_DASH_ = Pattern.compile("( - .*$)*", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanDirectionHeadsign(int directionId, boolean fromStopName, @NotNull String directionHeadSign) {
		directionHeadSign = super.cleanDirectionHeadsign(directionId, fromStopName, directionHeadSign);
		// ignore default trips head-signs
		directionHeadSign = INBOUND_OUTBOUND_.matcher(directionHeadSign).replaceAll(EMPTY);
		// clean stop name
		if (fromStopName) {
			directionHeadSign = STARTS_WITH_BOUNDS.matcher(directionHeadSign).replaceAll(EMPTY);
			directionHeadSign = TRANSIT_TERMINAL_.matcher(directionHeadSign).replaceAll(TRANSIT_TERMINAL_REPLACEMENT);
			directionHeadSign = KEEP_INSIDE_PARENTHESES.matcher(directionHeadSign).replaceAll(KEEP_INSIDE_PARENTHESES_REPLACEMENT);
			directionHeadSign = ENDS_WITH_DASH_.matcher(directionHeadSign).replaceAll(EMPTY);
		}
		return directionHeadSign;
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.cleanSlashes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.CLEAN_AND.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.cleanSlashes(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		if (!StringUtils.isEmpty(gStop.getStopCode())
				&& CharUtils.isDigitsOnly(gStop.getStopCode())) {
			return Integer.parseInt(gStop.getStopCode()); // use stop code as stop ID
		}
		throw new MTLog.Fatal("Unexpected stop ID for %s!", gStop);
	}
}
