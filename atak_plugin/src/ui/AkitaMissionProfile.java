package com.akitaengineering.meshtak.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public final class AkitaMissionProfile {

    public static final String PREF_MISSION_PROFILE = "mission_profile";
    public static final String PROFILE_SEARCH_RESCUE = "search_rescue";
    public static final String PROFILE_LAW_ENFORCEMENT = "law_enforcement";
    public static final String PROFILE_COAST_GUARD = "coast_guard";
    public static final String PROFILE_MILITARY = "military";
    public static final String PROFILE_PRIVATE_SECURITY = "private_security";

    private AkitaMissionProfile() {
    }

    public static String getProfile(Context context) {
        return getProfile(PreferenceManager.getDefaultSharedPreferences(context));
    }

    public static String getProfile(SharedPreferences preferences) {
        return preferences.getString(PREF_MISSION_PROFILE, PROFILE_SEARCH_RESCUE);
    }

    public static String getProfileLabel(Context context) {
        return getProfileLabel(getProfile(context));
    }

    public static String getProfileLabel(SharedPreferences preferences) {
        return getProfileLabel(getProfile(preferences));
    }

    public static String getProfileLabel(String profile) {
        if (PROFILE_LAW_ENFORCEMENT.equals(profile)) {
            return "Law Enforcement";
        }
        if (PROFILE_COAST_GUARD.equals(profile)) {
            return "Coast Guard";
        }
        if (PROFILE_MILITARY.equals(profile)) {
            return "Military";
        }
        if (PROFILE_PRIVATE_SECURITY.equals(profile)) {
            return "Private Security";
        }
        return "Search & Rescue";
    }

    public static String getProfileBadge(SharedPreferences preferences) {
        String profile = getProfile(preferences);
        if (PROFILE_LAW_ENFORCEMENT.equals(profile)) {
            return "LE Ops";
        }
        if (PROFILE_COAST_GUARD.equals(profile)) {
            return "Coast Guard";
        }
        if (PROFILE_MILITARY.equals(profile)) {
            return "Defense";
        }
        if (PROFILE_PRIVATE_SECURITY.equals(profile)) {
            return "Private Security";
        }
        return "SAR";
    }

    public static String getOperationalFocus(SharedPreferences preferences) {
        String profile = getProfile(preferences);
        if (PROFILE_LAW_ENFORCEMENT.equals(profile)) {
            return "perimeter integrity, officer safety, and evidentiary reporting";
        }
        if (PROFILE_COAST_GUARD.equals(profile)) {
            return "maritime search, vessel accountability, and boarding coordination";
        }
        if (PROFILE_MILITARY.equals(profile)) {
            return "maneuver synchronization, survivability, and contested-environment reporting";
        }
        if (PROFILE_PRIVATE_SECURITY.equals(profile)) {
            return "site integrity, guard force coordination, and incident escalation";
        }
        return "search grids, casualty updates, and extraction coordination";
    }

    public static String getTemplateHint(SharedPreferences preferences) {
        String profile = getProfile(preferences);
        if (PROFILE_LAW_ENFORCEMENT.equals(profile)) {
            return "Playbooks emphasize perimeter status, subject updates, and rapid command requests.";
        }
        if (PROFILE_COAST_GUARD.equals(profile)) {
            return "Playbooks emphasize vessel identity, search sector handoffs, and maritime escalation.";
        }
        if (PROFILE_MILITARY.equals(profile)) {
            return "Playbooks emphasize SITREP, logistics requests, and command-and-control brevity.";
        }
        if (PROFILE_PRIVATE_SECURITY.equals(profile)) {
            return "Playbooks emphasize patrol checkpoints, access control events, and escalation notices.";
        }
        return "Playbooks emphasize search progress, medical updates, and extraction coordination.";
    }

    public static List<TemplatePreset> getTemplatePresets(SharedPreferences preferences) {
        return getTemplatePresets(getProfile(preferences));
    }

    public static List<TemplatePreset> getTemplatePresets(String profile) {
        List<TemplatePreset> presets = new ArrayList<>();
        if (PROFILE_LAW_ENFORCEMENT.equals(profile)) {
            presets.add(new TemplatePreset(
                    "Officer Check-In",
                    "JSON",
                    "{\"type\":\"officer_checkin\",\"sector\":\"Bravo-2\",\"priority\":\"routine\",\"status\":\"Perimeter holding, no active threat indicators.\"}"));
            presets.add(new TemplatePreset(
                    "Perimeter Shift",
                    "Plain Text",
                    "Perimeter shift complete. North and east access points now covered. Camera sweep clean."));
            presets.add(new TemplatePreset(
                    "Support Request",
                    "Custom",
                    "REQUEST:SUPPORT_UNIT:PERIMETER_REINFORCEMENT"));
            return presets;
        }

        if (PROFILE_COAST_GUARD.equals(profile)) {
            presets.add(new TemplatePreset(
                    "Vessel Sighting",
                    "JSON",
                    "{\"type\":\"vessel_spotrep\",\"sector\":\"Alpha-4\",\"priority\":\"routine\",\"summary\":\"Unidentified vessel sighted on assigned search line, maintaining observation.\"}"));
            presets.add(new TemplatePreset(
                    "Boarding Update",
                    "Plain Text",
                    "Boarding team ready. Weather stable. Preparing compliant intercept and inspection sequence."));
            presets.add(new TemplatePreset(
                    "Sector Handoff",
                    "Custom",
                    "REQUEST:SECTOR_HANDOFF:MARITIME_SEARCH"));
            return presets;
        }

        if (PROFILE_MILITARY.equals(profile)) {
            presets.add(new TemplatePreset(
                    "SITREP",
                    "JSON",
                    "{\"type\":\"sitrep\",\"grid\":\"11U PU 34567 89012\",\"priority\":\"routine\",\"summary\":\"Element in position, mobility green, no contact reported.\"}"));
            presets.add(new TemplatePreset(
                    "Logistics Request",
                    "Plain Text",
                    "Logistics request follows: water, batteries, and medical replenishment required at next secure resupply window."));
            presets.add(new TemplatePreset(
                    "Command Sync",
                    "Custom",
                    "REQUEST:C2_SYNC:MISSION_PHASE_UPDATE"));
            return presets;
        }

        if (PROFILE_PRIVATE_SECURITY.equals(profile)) {
            presets.add(new TemplatePreset(
                    "Patrol Checkpoint",
                    "JSON",
                    "{\"type\":\"checkpoint\",\"site\":\"North Gate\",\"priority\":\"routine\",\"summary\":\"Checkpoint clear, badge verification normal, no intrusion indicators.\"}"));
            presets.add(new TemplatePreset(
                    "Incident Note",
                    "Plain Text",
                    "Incident note: suspicious vehicle observed outside perimeter. Monitoring and logging until relieved."));
            presets.add(new TemplatePreset(
                    "Escalation Request",
                    "Custom",
                    "REQUEST:ESCALATE:SUPERVISOR_RESPONSE"));
            return presets;
        }

        presets.add(new TemplatePreset(
                "Team Spot Report",
                "JSON",
                "{\"type\":\"spotrep\",\"grid\":\"11U PU 34567 89012\",\"priority\":\"routine\",\"summary\":\"Search team check-in complete, route conditions stable, no casualty contact yet.\"}"));
        presets.add(new TemplatePreset(
                "Medical Update",
                "Plain Text",
                "Medical update: casualty stabilized, litter team staged, landing zone assessment in progress."));
        presets.add(new TemplatePreset(
                "Extraction Request",
                "Custom",
                "REQUEST:EXTRACTION_SUPPORT:TEAM_ALPHA"));
        return presets;
    }

    public static final class TemplatePreset {
        public final String label;
        public final String format;
        public final String payload;

        TemplatePreset(String label, String format, String payload) {
            this.label = label;
            this.format = format;
            this.payload = payload;
        }
    }
}