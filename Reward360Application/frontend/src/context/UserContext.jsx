import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import userService from '../services/userService';

const UserContext = createContext(null);

export const useUser = () => {
  const context = useContext(UserContext);
  if (!context) throw new Error('useUser must be used within a UserProvider');
  return context;
};

export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [offers, setOffers] = useState([]);
  const [redemptions, setRedemptions] = useState([]);
  const [loading, setLoading] = useState(true);

  // Helper to check if we are logged in
  const isAuthenticated = () => !!localStorage.getItem('userId');

  const refreshAll = useCallback(async () => {
    // Stop if there is no user ID to prevent 400 errors
    if (!isAuthenticated()) {
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      // Fetch all user-specific data in parallel
      const [userData, transData, offersData, redeemData] = await Promise.all([
        userService.getMe(),
        userService.getTransactions(),
        userService.getOffersForMyTier(),
        userService.getRedemptions()
      ]);

      setUser(userData);
      setTransactions(transData);
      setOffers(offersData);
      setRedemptions(redeemData);
    } catch (err) {
      console.error('UserContext: Sync failed', err);
    } finally {
      setLoading(false);
    }
  }, []);

  // Claim points and update UI immediately
  const claimPoints = async (code, pts, note) => {
    await userService.claimPoints(code, pts, note);
    await refreshAll(); // Refresh to update point balance on screen
  };

  // Redeem offer and update UI immediately
  const redeemOffer = async (offerId, store) => {
    await userService.redeemOffer(offerId, store);
    await refreshAll(); // Refresh to show new redemption in list
  };

  // Run on initial page load
  useEffect(() => {
    refreshAll();
  }, [refreshAll]);

  const value = {
    user,
    transactions,
    offers,
    redemptions,
    loading,
    refreshAll,
    claimPoints,
    redeemOffer
  };

  return <UserContext.Provider value={value}>{children}</UserContext.Provider>;
};