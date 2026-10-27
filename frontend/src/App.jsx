import { useState } from 'react'
import { api, setAuth } from './api'

function Home({ go }) {   // the FRONT PAGE shown in Figure 2 of the report
  return (<>
    <nav className="navbar bg-light px-4 border-bottom">
      <span className="navbar-brand fw-bold">SecureHealth Cloud</span>
      <div className="d-flex gap-3">
        <b>Home</b>
        {['Admin','Data Owner','Data User','Authority'].map(r =>
          <a key={r} href="#" onClick={e => { e.preventDefault(); go('login', r) }}>{r} Login</a>)}
        <a href="#" onClick={e => { e.preventDefault(); go('register') }}>Register</a>
      </div>
    </nav>
    <div className="container py-5">
      <div className="row"><div className="col-md-8">
        <h1 className="fw-bold">Secure Healthcare Data Management using Attribute-Based Searchable Encryption and Cloud Computing</h1>
        <p className="text-secondary mt-3">Records are encrypted with ABSE before they are stored on AWS. Only authorized healthcare professionals can search and request them, and every action is logged.</p>
        <button className="btn btn-primary" onClick={() => go('login', 'Data User')}>GET STARTED</button>
      </div></div>
      <div className="row mt-5 g-3 text-center">
        {['Encrypted Cloud Storage','Attribute-Based Access Control','Hash Integrity & Audit Logs'].map(t =>
          <div className="col-md-4" key={t}><div className="card p-4 fw-semibold">{t}</div></div>)}
      </div>
    </div></>)
}

function Register({ go }) {
  const [f, setF] = useState({ username: '', password: '', role: 'DATA_USER', attributes: '' })
  const [msg, setMsg] = useState('')
  const submit = async () => {
    try { await api.post('/auth/register', f); setMsg('Registered. Please login.') }
    catch (e) { setMsg(e.response?.data?.message || 'Failed') } }
  return (<div className="container py-5" style={{ maxWidth: 420 }}><h3>Register</h3>
    <input className="form-control my-2" placeholder="Username" onChange={e => setF({ ...f, username: e.target.value })} />
    <input className="form-control my-2" type="password" placeholder="Password" onChange={e => setF({ ...f, password: e.target.value })} />
    <select className="form-select my-2" onChange={e => setF({ ...f, role: e.target.value })}>
      <option value="DATA_USER">Data User</option><option value="DATA_OWNER">Data Owner</option><option value="AUTHORITY">Authority</option></select>
    <input className="form-control my-2" placeholder="Attributes e.g. doctor,cardiology" onChange={e => setF({ ...f, attributes: e.target.value })} />
    <button className="btn btn-primary me-2" onClick={submit}>Register</button>
    <button className="btn btn-link" onClick={() => go('home')}>Back</button>
    <div className="mt-2 text-success">{msg}</div></div>)
}

function Login({ role, onLogin, go }) {
  const [u, setU] = useState(''); const [p, setP] = useState(''); const [err, setErr] = useState('')
  const submit = async () => {
    try { setAuth(u, p); const r = await api.get('/auth/me'); onLogin(r.data) }
    catch { setAuth(null); setErr('Invalid credentials') } }
  return (<div className="container py-5" style={{ maxWidth: 380 }}><h3>{role} Login</h3>
    <input className="form-control my-2" placeholder="Username" onChange={e => setU(e.target.value)} />
    <input className="form-control my-2" type="password" placeholder="Password" onChange={e => setP(e.target.value)} />
    <button className="btn btn-primary me-2" onClick={submit}>Login</button>
    <button className="btn btn-link" onClick={() => go('home')}>Back</button>
    <div className="mt-2 text-danger">{err}</div></div>)
}

function Owner() {
  const [file, setFile] = useState(null); const [policy, setPolicy] = useState(''); const [kw, setKw] = useState(''); const [msg, setMsg] = useState('')
  const upload = async () => {
    const fd = new FormData(); fd.append('file', file); fd.append('policy', policy); fd.append('keywords', kw)
    try { const r = await api.post('/records', fd); setMsg('Encrypted & uploaded. Record ID ' + r.data.id) }
    catch { setMsg('Upload failed') } }
  return (<div><h4>Upload EHR</h4>
    <input type="file" className="form-control my-2" onChange={e => setFile(e.target.files[0])} />
    <input className="form-control my-2" placeholder="Access policy e.g. doctor,cardiology" onChange={e => setPolicy(e.target.value)} />
    <input className="form-control my-2" placeholder="Search keywords e.g. diabetes,patient101" onChange={e => setKw(e.target.value)} />
    <button className="btn btn-success" onClick={upload}>Encrypt & Upload</button><div className="mt-2">{msg}</div></div>)
}

function DataUser() {
  const [kw, setKw] = useState(''); const [rows, setRows] = useState([]); const [msg, setMsg] = useState('')
  const search = async () => { const r = await api.get('/records/search', { params: { keyword: kw } }); setRows(r.data) }
  const request = async id => {
    try { await api.post(`/records/${id}/request`); setMsg('Request sent (PENDING)') }
    catch (e) { setMsg(e.response?.data?.message || 'Failed') } }
  const download = async (id, name) => {
    try {
      const r = await api.get(`/records/${id}/download`, { responseType: 'blob' })
      const a = document.createElement('a'); a.href = URL.createObjectURL(r.data); a.download = name; a.click()
    } catch { setMsg('Download blocked: not approved yet, or integrity check failed') } }
  return (<div><h4>Search Records</h4>
    <div className="input-group my-2"><input className="form-control" placeholder="keyword" onChange={e => setKw(e.target.value)} />
      <button className="btn btn-primary" onClick={search}>Search</button></div>
    <div className="text-info">{msg}</div>
    <table className="table"><tbody>{rows.map(r => <tr key={r.id}><td>{r.fileName}</td><td>{r.policy}</td>
      <td><button className="btn btn-sm btn-outline-primary me-2" onClick={() => request(r.id)}>Request</button>
          <button className="btn btn-sm btn-outline-success" onClick={() => download(r.id, r.fileName)}>Download</button></td></tr>)}</tbody></table></div>)
}

function Authority() {
  const [rows, setRows] = useState([])
  const load = async () => setRows((await api.get('/requests/pending')).data)
  const decide = async (id, approve) => { await api.post(`/requests/${id}/decision`, null, { params: { approve } }); load() }
  return (<div><h4>Pending Requests</h4><button className="btn btn-secondary mb-2" onClick={load}>Refresh</button>
    <table className="table"><tbody>{rows.map(r => <tr key={r.id}><td>{r.username}</td><td>{r.fileName}</td>
      <td><button className="btn btn-sm btn-success me-2" onClick={() => decide(r.id, true)}>Approve</button>
          <button className="btn btn-sm btn-danger" onClick={() => decide(r.id, false)}>Reject</button></td></tr>)}</tbody></table></div>)
}

function Admin() {
  const [users, setUsers] = useState([]); const [logs, setLogs] = useState([])
  const load = async () => { setUsers((await api.get('/admin/users')).data); setLogs((await api.get('/admin/logs')).data) }
  return (<div><button className="btn btn-secondary mb-3" onClick={load}>Load users & audit log</button>
    <h5>Users</h5><ul>{users.map(u => <li key={u.id}>{u.username} - {u.role} - {u.attributes}</li>)}</ul>
    <h5>Audit log</h5><table className="table table-sm"><tbody>{logs.map(l =>
      <tr key={l.id}><td>{l.at}</td><td>{l.username}</td><td>{l.action}</td><td>{l.detail}</td></tr>)}</tbody></table></div>)
}

export default function App() {
  const [page, setPage] = useState('home'); const [role, setRole] = useState(''); const [user, setUser] = useState(null)
  const go = (p, r) => { setPage(p); if (r) setRole(r) }
  if (user) return (<div className="container py-4"><div className="d-flex justify-content-between mb-3">
      <h3>{user.role.replace('_', ' ')} Dashboard - {user.username}</h3>
      <button className="btn btn-outline-danger" onClick={() => { setAuth(null); setUser(null); setPage('home') }}>Logout</button></div>
    {user.role === 'DATA_OWNER' && <Owner />}{user.role === 'DATA_USER' && <DataUser />}
    {user.role === 'AUTHORITY' && <Authority />}{user.role === 'ADMIN' && <Admin />}</div>)
  if (page === 'login') return <Login role={role} go={go} onLogin={setUser} />
  if (page === 'register') return <Register go={go} />
  return <Home go={go} />
}
