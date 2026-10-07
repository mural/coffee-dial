const statuses = {
  invalid_backup: 422, invalid_ids: 422, invalid_bean: 422, invalid_machine: 422,
  invalid_cup: 422, invalid_shot: 422, invalid_protocol: 422,
  backup_upgrade_required: 426, sync_capacity: 413, payload_too_large: 413,
  unauthorized: 401, provider_unavailable: 503, service_unavailable: 503
};
export class ServiceError extends Error {
  constructor(code, status = statuses[code] || 503) { super(code); this.status = status; }
}
export function serviceError(error) {
  return error instanceof ServiceError ? error :
    new ServiceError(statuses[error?.message] ? error.message : 'service_unavailable');
}
