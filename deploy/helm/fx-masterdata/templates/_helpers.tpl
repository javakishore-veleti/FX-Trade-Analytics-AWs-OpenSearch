{{/*
Generic helpers for the fx-masterdata chart. Naming follows the
{{ .Release.Name }}-{{ .Chart.Name }} convention so multiple releases of
the same chart can coexist in one namespace.
*/}}

{{- define "fx-masterdata.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "fx-masterdata.serviceAccountName" -}}
{{- if .Values.serviceAccount.name -}}
{{ .Values.serviceAccount.name }}
{{- else -}}
{{ include "fx-masterdata.fullname" . }}
{{- end -}}
{{- end -}}

{{/*
Comma-joined Spring profiles for the Deployment (service pods).
*/}}
{{- define "fx-masterdata.podProfiles" -}}
{{- $base := .Values.spring.baseProfile -}}
{{- $extras := .Values.spring.podProfiles | join "," -}}
{{- if $extras -}}{{ printf "%s,%s" $base $extras }}{{- else -}}{{ $base }}{{- end -}}
{{- end -}}

{{/*
Comma-joined Spring profiles for the migration Job.
*/}}
{{- define "fx-masterdata.jobProfiles" -}}
{{- $base := .Values.spring.baseProfile -}}
{{- $extras := .Values.spring.jobProfiles | join "," -}}
{{- if $extras -}}{{ printf "%s,%s" $base $extras }}{{- else -}}{{ $base }}{{- end -}}
{{- end -}}

{{- define "fx-masterdata.image" -}}
{{- printf "%s:%s" .Values.image.repository .Values.image.tag -}}
{{- end -}}

{{/*
Common labels applied to every resource. Helm best practice.
*/}}
{{- define "fx-masterdata.labels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version }}
{{- end -}}

{{/*
Selector labels — narrower; only these can change without breaking
referential integrity between Deployment selector and Pod template.
*/}}
{{- define "fx-masterdata.selectorLabels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
