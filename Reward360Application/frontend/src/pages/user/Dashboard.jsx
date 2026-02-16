import React, { useState, useEffect } from 'react'
import { useUser } from '../../context/UserContext'
import '../../styles/Dashboard.css'

function ClaimCard({ a, onClaimed, isClaimed }) {
  const [busy, setBusy] = useState(false)
  const { claimPoints } = useUser()

  const claim = async () => {
    setBusy(true)
    try {
      await claimPoints(a.code, a.points, a.title)
      onClaimed(a.code)
    } catch (error) {
      console.error('Failed to claim:', error)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="d-card d-claim-card">
      <h4 className="d-card-title">{a.title}</h4>
      <p>Earn {a.points} points</p>
      {!isClaimed ? (
        <button disabled={busy} className="d-btn" onClick={claim}>
          {busy ? 'Claiming…' : 'Claim'}
        </button>
      ) : (
        <span className="d-badge">Claimed</span>
      )}
    </div>
  )
}

export default function Dashboard() {
  const { user, transactions, loading } = useUser()
  const [claimedActivities, setClaimedActivities] = useState([])

  // Ensure storage is unique per user to prevent data bleed
  const userStorageKey = user ? `claimedActivities_${user.userId}` : null

  useEffect(() => {
    if (userStorageKey) {
      const saved = sessionStorage.getItem(userStorageKey)
      setClaimedActivities(saved ? JSON.parse(saved) : [])
    }
  }, [userStorageKey])

  useEffect(() => {
    if (userStorageKey) {
      sessionStorage.setItem(userStorageKey, JSON.stringify(claimedActivities))
    }
  }, [claimedActivities, userStorageKey])

  if (loading || !user) return <div className="d-page">Loading...</div>

  const activities = [
    { title: 'Daily Login Bonus', points: 50, code: 'LOGIN' },
    { title: 'Write a Product Review', points: 100, code: 'REVIEW' },
    { title: 'Share on Social', points: 75, code: 'SOCIAL' },
    { title: 'Refer a Friend', points: 200, code: 'REFER' },
  ]

  return (
    <div className="d-page">
      {/* Points Summary Section */}
      <div className="d-card d-ps">
        <div className="d-ps-row">
          <div className="d-ps-left">
            <h3 className="d-ps-title">Points Summary</h3>
            <p className="d-ps-sub">
              Member: <span className="d-ps-strong">{user.customerName}</span>
              <span className="d-ps-dot">·</span>
              Tier: <span className="d-ps-strong">{user.loyaltyTier}</span>
            </p>
          </div>
          <div className="d-ps-right">
            <div className="d-ps-right-label">Current Balance</div>
            <div className="d-ps-right-value">{user.pointsBalance ?? 0}</div>
          </div>
        </div>
      </div>

      {/* Activities Grid */}
      <div className="d-card">
        <h3 className="d-section-title">Daily Activities</h3>
        <div className="d-activities-row">
          {activities.map(a => (
            <ClaimCard 
              key={a.code} 
              a={a} 
              onClaimed={(code) => setClaimedActivities(prev => [...prev, code])}
              isClaimed={claimedActivities.includes(a.code)}
            />
          ))}
        </div>
      </div>

      {/* Transaction Table */}
      <div className="d-card">
        <h3 className="d-section-title">Recent Transactions</h3>
        <div className="d-table-wrap">
          <table className="d-table">
            <thead>
              <tr><th>Date</th><th>Type</th><th>Earned</th><th>Redeemed</th><th>Detail</th></tr>
            </thead>
            <tbody>
              {transactions.map(t => (
                <tr key={t.id}>
                  <td>{t.date}</td>
                  <td>{t.type}</td>
                  <td style={{ color: '#059669', fontWeight: '700' }}>{t.pointsEarned || '—'}</td>
                  <td style={{ color: '#dc2626', fontWeight: '700' }}>{t.pointsRedeemed || '—'}</td>
                  <td>{t.store || t.note}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}