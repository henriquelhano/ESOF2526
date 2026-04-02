<template>
  <v-dialog v-model="dialog" persistent width="800">
    <v-card>
      <v-card-title>
        <span class="headline">
          {{
            editEnrollment && editEnrollment.id === null
              ? 'New Application'
              : 'Edit Application'
          }}
        </span>
      </v-card-title>
      <v-card-text>
        <v-form ref="form" lazy-validation>
          <v-row v-if="editEnrollment.id === null">
            <v-col cols="12">
              <v-select
                label="*Shifts"
                :items="availableShifts"
                :item-text="(s) => s.formattedStartTime + ' → ' + s.formattedEndTime"
                item-value="id"
                v-model="editEnrollment.shiftIds"
                multiple
                chips
                :rules="[(v) => v.length >= 1 || 'Select at least one shift']"
                data-cy="shiftSelect"
              ></v-select>
              <v-row v-if="hasOverlappingShifts">
                <v-col cols="12">
                  <v-alert type="error" data-cy="overlapError">
                    Selected shifts have overlapping periods.
                  </v-alert>
                </v-col>
              </v-row>
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="12">
              <v-textarea
                label="*Motivation"
                :rules="[(v) => !!v || 'Motivation is required']"
                required
                v-model="editEnrollment.motivation"
                data-cy="motivationInput"
                auto-grow
                rows="1"
              ></v-textarea>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
          color="blue-darken-1"
          variant="text"
          @click="$emit('close-enrollment-dialog')"
        >
          Close
        </v-btn>
        <v-btn
          v-if="canSave"
          color="blue-darken-1"
          variant="text"
          @click="updateEnrollment"
          data-cy="saveEnrollment"
        >
          Save
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script lang="ts">
import { Vue, Component, Prop, Model } from 'vue-property-decorator';
import RemoteServices from '@/services/RemoteServices';
import { ISOtoString } from '@/services/ConvertDateService';
import Enrollment from '@/models/enrollment/Enrollment';
import Shift from '@/models/shift/Shift';

@Component({
  methods: { ISOtoString },
})
export default class EnrollmentDialog extends Vue {
  @Model('dialog', Boolean) dialog!: boolean;
  @Prop({ type: Enrollment, required: true }) readonly enrollment!: Enrollment;
  @Prop({ type: Array, required: true }) readonly shifts!: Shift[];

  editEnrollment: Enrollment = new Enrollment();

  async created() {
    this.editEnrollment = new Enrollment(this.enrollment);
  }

  get canSave(): boolean {
    const isEdit = this.editEnrollment.id !== null;
    return (
      !!this.editEnrollment.motivation &&
      this.editEnrollment.motivation.length >= 10 &&
      (isEdit || (this.editEnrollment.shiftIds.length >= 1 && !this.hasOverlappingShifts))
    );
  }

  get availableShifts(): Shift[] {
    return this.shifts;
  }

  get hasOverlappingShifts(): boolean {
    const selectedShifts = this.availableShifts.filter(shift =>
      shift.id !== null && this.editEnrollment.shiftIds.includes(shift.id)
    );
    return this.checkForOverlaps(selectedShifts);
  }

  private checkForOverlaps(shifts: Shift[]): boolean {
    for (let i = 0; i < shifts.length; i++) {
      for (let j = i + 1; j < shifts.length; j++) {
        const shift1 = shifts[i];
        const shift2 = shifts[j];
        const start1 = new Date(shift1.startTime);
        const end1 = new Date(shift1.endTime);
        const start2 = new Date(shift2.startTime);
        const end2 = new Date(shift2.endTime);
        if (start1 < end2 && start2 < end1) {
          return true;
        }
      }
    }
    return false;
  }

  async updateEnrollment() {
    // editar
    if (
      this.editEnrollment.id !== null &&
      (this.$refs.form as Vue & { validate: () => boolean }).validate()
    ) {
      try {
        const result = await RemoteServices.editEnrollment(
          this.editEnrollment.id,
          this.editEnrollment,
        );
        this.$emit('update-enrollment', result);
      } catch (error) {
        await this.$store.dispatch('error', error);
      }
    }
    // criar
    else if (
      this.editEnrollment.activityId !== null &&
      (this.$refs.form as Vue & { validate: () => boolean }).validate()
    ) {
      try {
        const result = await RemoteServices.createEnrollment(
          this.editEnrollment,
        );
        this.$emit('save-enrollment', result);
      } catch (error) {
        await this.$store.dispatch('error', error);
      }
    }
  }
}
</script>

<style scoped lang="scss"></style>