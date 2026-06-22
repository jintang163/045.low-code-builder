import request from '@/utils/request'

export interface ShiftConfig {
  id: number
  appId: number
  shiftCode: string
  shiftName: string
  shiftColor: string
  startTime: string
  endTime: string
  workHours: number
  hourlyWage: number
  sortOrder: number
  status: number
}

export interface ShiftSchedule {
  id: number
  appId: number
  userId: number
  userName: string
  shiftType: string
  shiftDate: string
  startTime: string
  endTime: string
  workHours: number
  hourlyWage: number
  remark?: string
}

export interface AttendanceRecord {
  id: number
  appId: number
  userId: number
  userName: string
  attendanceDate: string
  clockInTime?: string
  clockOutTime?: string
  clockInLatitude?: number
  clockInLongitude?: number
  clockOutLatitude?: number
  clockOutLongitude?: number
  clockInLocation?: string
  clockOutLocation?: string
  workHours: number
  status: string
  lateMinutes: number
  earlyMinutes: number
  shiftType?: string
  remark?: string
}

export interface LeaveRequest {
  id: number
  appId: number
  userId: number
  userName: string
  leaveType: string
  startDate: string
  endDate: string
  startTime?: string
  endTime?: string
  leaveDays: number
  leaveHours: number
  reason: string
  attachmentUrl?: string
  approverId?: number
  approverName?: string
  approvalTime?: string
  approvalRemark?: string
  status: string
  createdAt: string
}

export interface SalaryRecord {
  id: number
  appId: number
  userId: number
  userName: string
  salaryMonth: string
  totalWorkDays: number
  totalWorkHours: number
  baseSalary: number
  hourlyWage: number
  overtimeHours: number
  overtimePay: number
  leaveDays: number
  leaveDeduction: number
  lateDeduction: number
  earlyDeduction: number
  absentDeduction: number
  bonus: number
  subsidy: number
  deductionTotal: number
  netSalary: number
  remark?: string
  status: string
  paidTime?: string
}

export interface AttendanceStats {
  userId: number
  userName: string
  workDays: number
  totalHours: number
  lateCount: number
  earlyCount: number
  absentCount: number
  leaveDays: number
}

export const attendanceApi = {
  getShiftConfigs: (appId: number) =>
    request.get<ShiftConfig[]>('/attendance/shift/configs', { params: { appId } }),

  getUserSchedules: (appId: number, userId: number, startDate: string, endDate: string) =>
    request.get<ShiftSchedule[]>('/attendance/shift/user/list', {
      params: { appId, userId, startDate, endDate },
    }),

  getAppSchedules: (appId: number, startDate: string, endDate: string) =>
    request.get<ShiftSchedule[]>('/attendance/shift/app/list', {
      params: { appId, startDate, endDate },
    }),

  saveSchedule: (data: Partial<ShiftSchedule>) =>
    request.post<ShiftSchedule>('/attendance/shift', data),

  batchSchedule: (data: {
    appId: number
    userIds: number[]
    startDate: string
    endDate: string
    shiftType: string
    workHours?: number
    hourlyWage?: number
  }) => request.post<void>('/attendance/shift/batch', data),

  deleteSchedule: (appId: number, userId: number, shiftDate: string) =>
    request.delete<void>('/attendance/shift', { params: { appId, userId, shiftDate } }),

  clockIn: (data: { appId: number; latitude?: number; longitude?: number; location?: string }) =>
    request.post<AttendanceRecord>('/attendance/record/clock-in', data),

  clockOut: (data: { appId: number; latitude?: number; longitude?: number; location?: string }) =>
    request.post<AttendanceRecord>('/attendance/record/clock-out', data),

  getTodayRecord: (appId: number) =>
    request.get<AttendanceRecord>('/attendance/record/today', { params: { appId } }),

  getMyRecords: (appId: number, startDate: string, endDate: string) =>
    request.get<AttendanceRecord[]>('/attendance/record/my', {
      params: { appId, startDate, endDate },
    }),

  getUserRecords: (appId: number, userId: number, startDate: string, endDate: string) =>
    request.get<AttendanceRecord[]>('/attendance/record/user/list', {
      params: { appId, userId, startDate, endDate },
    }),

  getAppRecords: (appId: number, startDate: string, endDate: string) =>
    request.get<AttendanceRecord[]>('/attendance/record/app/list', {
      params: { appId, startDate, endDate },
    }),

  getAttendanceStats: (appId: number, startDate: string, endDate: string) =>
    request.get<AttendanceStats[]>('/attendance/record/stats', {
      params: { appId, startDate, endDate },
    }),

  createLeave: (data: Partial<LeaveRequest>) =>
    request.post<LeaveRequest>('/attendance/leave', data),

  approveLeave: (data: { id: number; status: string; approvalRemark?: string }) =>
    request.post<LeaveRequest>('/attendance/leave/approve', data),

  getMyLeaves: (appId: number) =>
    request.get<LeaveRequest[]>('/attendance/leave/my', { params: { appId } }),

  getPendingLeaves: (appId: number) =>
    request.get<LeaveRequest[]>('/attendance/leave/pending', { params: { appId } }),

  getLeavesByStatus: (appId: number, status?: string) =>
    request.get<LeaveRequest[]>('/attendance/leave/app/list', {
      params: { appId, status },
    }),

  generateSalary: (data: { appId: number; salaryMonth: string; defaultHourlyWage?: number }) =>
    request.post<SalaryRecord[]>('/attendance/salary/generate', data),

  getSalaryByMonth: (appId: number, salaryMonth: string) =>
    request.get<SalaryRecord[]>('/attendance/salary/month', {
      params: { appId, salaryMonth },
    }),

  getMySalary: (appId: number) =>
    request.get<SalaryRecord[]>('/attendance/salary/my', { params: { appId } }),

  markPaid: (appId: number, salaryMonth: string) =>
    request.post<void>('/attendance/salary/mark-paid', null, { params: { appId, salaryMonth } }),

  exportSalary: (appId: number, salaryMonth: string) =>
    request.get<Blob>('/attendance/salary/export', {
      params: { appId, salaryMonth },
      responseType: 'blob',
    }),
}
