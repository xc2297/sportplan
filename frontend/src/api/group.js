import api from './index'

export function getGroups() {
  return api.get('/groups')
}

export function getGroup(id) {
  return api.get(`/groups/${id}`)
}

export function createGroup(data) {
  return api.post('/groups', data)
}

export function requestJoinGroup(id) {
  return api.post(`/groups/${id}/join`)
}

export function leaveGroup() {
  return api.post('/groups/leave')
}

export function dissolveGroup(id) {
  return api.delete(`/groups/${id}`)
}

export function getGroupMembers(id) {
  return api.get(`/groups/${id}/members`)
}

export function setMemberAdmin(groupId, userId) {
  return api.post(`/groups/${groupId}/set-admin/${userId}`)
}

export function getPendingRequests(groupId) {
  return api.get(`/groups/${groupId}/join-requests`)
}

export function getMyRequests() {
  return api.get('/groups/my-requests')
}

export function approveRequest(requestId) {
  return api.post(`/groups/join-requests/${requestId}/approve`)
}

export function rejectRequest(requestId) {
  return api.post(`/groups/join-requests/${requestId}/reject`)
}
