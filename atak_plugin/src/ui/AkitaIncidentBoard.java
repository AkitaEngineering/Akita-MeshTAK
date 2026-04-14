package com.akitaengineering.meshtak.ui;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public final class AkitaIncidentBoard {

    private AkitaIncidentBoard() {
    }

    public static IncidentState getState(SharedPreferences preferences) {
        String profile = AkitaMissionProfile.getProfile(preferences);
        if (AkitaMissionProfile.PROFILE_LAW_ENFORCEMENT.equals(profile)) {
            return new IncidentState(
                    "Urban Containment",
                    "Rapid perimeter lockdown with officer-safe handoffs and evidentiary discipline.",
                    "LE Command Post",
                    "Accelerated",
                    "Confirm north and east containment handoff.",
                    "Containment sectors",
                    "Inner and outer cordon overlays with stale-unit callouts.");
        }
        if (AkitaMissionProfile.PROFILE_COAST_GUARD.equals(profile)) {
            return new IncidentState(
                    "Maritime Search",
                    "Track vessel sightings, sector coverage, and boarding readiness across the operating picture.",
                    "Boarding Team",
                    "Steady",
                    "Advance sector handoff for Alpha and Delta lanes.",
                    "Search lanes",
                    "Maritime search sectors with exclusion fence around intercept zone.");
        }
        if (AkitaMissionProfile.PROFILE_MILITARY.equals(profile)) {
            return new IncidentState(
                    "Tactical Sync",
                    "Maintain route health, logistics tempo, and stale-node visibility in contested conditions.",
                    "Tactical Operations",
                    "High",
                    "Issue updated C2 synchronization request.",
                    "Control sectors",
                    "Engagement-control wedges with maneuver fence and stale-node alerts.");
        }
        if (AkitaMissionProfile.PROFILE_PRIVATE_SECURITY.equals(profile)) {
            return new IncidentState(
                    "Facility Shield",
                    "Keep site perimeter, checkpoint activity, and supervisor escalation aligned on one board.",
                    "Site Command",
                    "Elevated",
                    "Validate north gate escalation route.",
                    "Patrol sectors",
                    "Property fence with checkpoint sectors and stale-guard alerts.");
        }
        return new IncidentState(
                "Search Grid",
                "Coordinate team lanes, casualty reporting, and extraction readiness across the mission picture.",
                "SAR Field Team",
                "Responsive",
                "Prepare extraction support update for Team Alpha.",
                "Search sectors",
                "Search wedges centered on the active team with casualty containment fence.");
    }

    public static List<RoleAction> getRoleActions(SharedPreferences preferences) {
        String profile = AkitaMissionProfile.getProfile(preferences);
        List<RoleAction> actions = new ArrayList<>();
        if (AkitaMissionProfile.PROFILE_LAW_ENFORCEMENT.equals(profile)) {
            actions.add(new RoleAction("Containment Update", "JSON", "{\"type\":\"containment_update\",\"sector\":\"Bravo-2\",\"priority\":\"high\",\"summary\":\"Inner cordon established, outer cordon shifting to traffic control.\"}"));
            actions.add(new RoleAction("Supervisor Request", "Custom", "REQUEST:SUPERVISOR:SCENE_COMMAND"));
            actions.add(new RoleAction("Evidence Escort", "Plain Text", "Evidence escort requested. Secure transport lane and preserve scene integrity."));
            return actions;
        }
        if (AkitaMissionProfile.PROFILE_COAST_GUARD.equals(profile)) {
            actions.add(new RoleAction("Sector Sweep", "JSON", "{\"type\":\"sector_sweep\",\"sector\":\"Delta\",\"priority\":\"routine\",\"summary\":\"Sweep complete, no additional vessel contacts.\"}"));
            actions.add(new RoleAction("Boarding Greenlight", "Custom", "REQUEST:BOARDING:GREENLIGHT"));
            actions.add(new RoleAction("Weather Escalation", "Plain Text", "Sea state increasing. Tighten search lanes and confirm crew endurance windows."));
            return actions;
        }
        if (AkitaMissionProfile.PROFILE_MILITARY.equals(profile)) {
            actions.add(new RoleAction("Command Sync", "JSON", "{\"type\":\"c2_sync\",\"priority\":\"high\",\"summary\":\"Element aligned on phase line, requesting updated mission timings.\"}"));
            actions.add(new RoleAction("Resupply Request", "Custom", "REQUEST:RESUPPLY:BATTERY_MEDICAL_WATER"));
            actions.add(new RoleAction("Contact Report", "Plain Text", "Contact report follows: no engagement, observation only, route remains usable."));
            return actions;
        }
        if (AkitaMissionProfile.PROFILE_PRIVATE_SECURITY.equals(profile)) {
            actions.add(new RoleAction("Checkpoint Status", "JSON", "{\"type\":\"checkpoint_status\",\"site\":\"North Gate\",\"priority\":\"routine\",\"summary\":\"Badge flow normal, no unauthorized entries detected.\"}"));
            actions.add(new RoleAction("Escalate Supervisor", "Custom", "REQUEST:ESCALATE:SUPERVISOR"));
            actions.add(new RoleAction("Patrol Alert", "Plain Text", "Patrol alert: suspicious loitering outside east perimeter, camera tasking adjusted."));
            return actions;
        }
        actions.add(new RoleAction("Extraction Update", "JSON", "{\"type\":\"extraction_update\",\"grid\":\"11U PU 34567 89012\",\"priority\":\"high\",\"summary\":\"Casualty package stable, landing zone marked, litter team moving.\"}"));
        actions.add(new RoleAction("Medical Escalation", "Custom", "REQUEST:MEDEVAC:TEAM_ALPHA"));
        actions.add(new RoleAction("Search Lane Shift", "Plain Text", "Shift search lanes west by one block. Weather and terrain are reducing visibility on current route."));
        return actions;
    }

    public static final class IncidentState {
        public final String title;
        public final String summary;
        public final String rolePack;
        public final String tempo;
        public final String nextAction;
        public final String overlayLabel;
        public final String overlaySummary;

        private IncidentState(String title,
                              String summary,
                              String rolePack,
                              String tempo,
                              String nextAction,
                              String overlayLabel,
                              String overlaySummary) {
            this.title = title;
            this.summary = summary;
            this.rolePack = rolePack;
            this.tempo = tempo;
            this.nextAction = nextAction;
            this.overlayLabel = overlayLabel;
            this.overlaySummary = overlaySummary;
        }
    }

    public static final class RoleAction {
        public final String label;
        public final String format;
        public final String payload;

        private RoleAction(String label, String format, String payload) {
            this.label = label;
            this.format = format;
            this.payload = payload;
        }
    }
}