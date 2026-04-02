<template>
  <v-dialog v-model="dialog" persistent width="1300">
    <v-card style="min-height: 600px; display: flex; flex-direction: column;">
      <v-card-title>
        <span class="headline">New Shift</span>
      </v-card-title>
      <v-card-text style="flex: 1;">
        <v-form ref="form" lazy-validation>
          <v-row>
            <v-col cols="12">
              <v-text-field
                label="*Location"
                :rules="[
                  (v) => !!v || 'Location is required',
                  (v) => (v && v.length >= 20) || 'Location must have at least 20 characters',
                  (v) => (v && v.length <= 200) || 'Location must have at most 200 characters',
                ]"
                required
                v-model="editShift.location"
                data-cy="locationInput"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6">
              <VueCtkDateTimePicker
                id="startTimeInput"
                v-model="editShift.startTime"
                format="YYYY-MM-DDTHH:mm:ssZ"
                label="*Start Time"
              ></VueCtkDateTimePicker>
            </v-col>
            <v-col cols="12" sm="6">
              <VueCtkDateTimePicker
                id="endTimeInput"
                v-model="editShift.endTime"
                format="YYYY-MM-DDTHH:mm:ssZ"
                label="*End Time"
              ></VueCtkDateTimePicker>
            </v-col>
            <v-col cols="12" sm="6">
              <v-text-field
                label="*Participants Limit"
                :rules="[
                  (v) => !!v || 'Participants limit is required',
                  (v) => (Number.isInteger(Number(v)) && Number(v) >= 1) || 'Must be a positive integer',
                ]"
                required
                v-model="editShift.participantsLimit"
                data-cy="participantsLimitInput"
              ></v-text-field>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
          color="blue-darken-1"
          variant="text"
          @click="$emit('close-shift-dialog')"
        >
          Close
        </v-btn>
        <v-btn
          :disabled="!canSave"
          color="blue-darken-1"
          variant="text"
          @click="saveShift"
          data-cy="saveShift"
        >
          Save
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script lang="ts">
import { Vue, Component, Prop, Model } from 'vue-property-decorator';
import Shift from '@/models/shift/Shift';
import Activity from '@/models/activity/Activity';
import RemoteServices from '@/services/RemoteServices';
import VueCtkDateTimePicker from 'vue-ctk-date-time-picker';
import 'vue-ctk-date-time-picker/dist/vue-ctk-date-time-picker.css';

Vue.component('VueCtkDateTimePicker', VueCtkDateTimePicker);

@Component
export default class ShiftDialog extends Vue {
  @Model('dialog', Boolean) dialog!: boolean;
  @Prop({ type: Shift, required: true }) readonly shift!: Shift;
  @Prop({ type: Activity, required: true }) readonly activity!: Activity;

  editShift: Shift = new Shift();

  async created() {
    this.editShift = new Shift(this.shift);
  }

  get canSave(): boolean {
    return (
      this.activity.state === 'APPROVED' &&
      !!this.editShift.location &&
      this.editShift.location.length >= 20 &&
      this.editShift.location.length <= 200 &&
      !!this.editShift.startTime &&
      !!this.editShift.endTime &&
      !!this.editShift.participantsLimit
    );
  }

  async saveShift() {
    if ((this.$refs.form as Vue & { validate: () => boolean }).validate()) {
      try {
        const result = await RemoteServices.createShift(
          this.activity.id!,
          this.editShift,
        );
        this.$emit('save-shift', result);
      } catch (error) {
        await this.$store.dispatch('error', error);
      }
    }
  }
}
</script>